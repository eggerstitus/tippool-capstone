package com.tippool

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Temporary print-packet recipient — change here when ready for production. */
const val CATERING_REPORT_EMAIL = "location-a@example.com" //fallBACK

data class OrderChecklist(
    val title: String,
    val subtitle: String,
    val itemLines: List<String>,
    val supplyLines: List<String>,
    val noteLines: List<String>,
)

data class PrintColumnBlock(
    val header: String,
    val lines: List<String>,
)

data class CateringPrintReport(
    val generatedAt: String,
    val statLine: String,
    val orderSummaries: List<String>,
    val headsUpLines: List<String>,
    val allSupplyLines: List<String>,
    val allItemSections: List<PrintColumnBlock>,
    val orderChecklists: List<OrderChecklist>,
)

fun buildCateringPrintReport(orders: List<CateringOrder>): CateringPrintReport {
    val allItems = orders.flatMap { it.items }
    val aggressive = shouldAggressiveHandoffConsolidation(allItems)
    val handoff = buildHandoffSections(allItems, aggressive)
    val cookies = cookieTotalsForItems(allItems)
    val boxes = totalCateringBoxes(orders)
    val generatedAt = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US).format(Date())

    val orderSummaries = orders.map { order ->
        val name = order.customerName.ifBlank { "Unnamed" }
        val time = order.time.ifBlank { "—" }
        val type = order.orderType.name.lowercase().replaceFirstChar { it.uppercase() }
        buildString {
            append("$name  ·  $time  ·  $type  ·  ${order.items.size} items")
            if (order.address.isNotBlank() && order.orderType == OrderType.DELIVERY) {
                append("\n    ${order.address}")
            }
        }
    }

    val headsUp = buildList {
        handoff.headsUp.forEach { add(it) }
        val deliveryNotes = deliveryNotesLines(orders)
        if (deliveryNotes.isNotEmpty()) {
            if (isNotEmpty()) add("")
            addAll(deliveryNotes)
        }
        // Note: cookie totals and box counts are already shown in the ALL SUPPLIES section,
        // so we don't duplicate them here.
    }

    val statLine = "${orders.size} order${if (orders.size != 1) "s" else ""}  ·  " +
            "${allItems.size} line item${if (allItems.size != 1) "s" else ""}"

    val itemSections = buildList {
        if (handoff.sandwiches.isNotEmpty())  add(PrintColumnBlock("SANDWICHES",  handoff.sandwiches))
        if (handoff.salads.isNotEmpty())      add(PrintColumnBlock("SALADS",      handoff.salads))
        if (handoff.pastas.isNotEmpty())      add(PrintColumnBlock("PASTAS",      handoff.pastas))
        if (handoff.other.isNotEmpty())       add(PrintColumnBlock("OTHER",       handoff.other))
        if (handoff.individual.isNotEmpty())  add(PrintColumnBlock("INDIVIDUAL",  handoff.individual))
        if (handoff.desserts.isNotEmpty())    add(PrintColumnBlock("DESSERT",     handoff.desserts))
        if (handoff.extras.isNotEmpty())      add(PrintColumnBlock("EXTRAS",      handoff.extras))
    }

    val supplyLines = buildCondensedSupplyLines(
        items = allItems,
        plateCount = orders.sumOf { it.plateCount },
        napkinCount = orders.sumOf { it.napkinCount },
        forksNeeded = orders.sumOf { it.forksNeeded },
        utensilTally = orders.fold(ServingUtensilTally()) { acc, order ->
            val t = order.tallyServingUtensils()
            ServingUtensilTally(
                acc.largeTongs + t.largeTongs,
                acc.smallTongs + t.smallTongs,
                acc.spoons + t.spoons,
            )
        },
        cateringBoxCount = boxes,
    ) + orders.flatMap { supplyNoteLinesForOrder(it, prefixCustomer = true) }

    val checklists = orders.map { order ->
        val name = order.customerName.ifBlank { "Unnamed Order" }
        val subtitle = buildString {
            if (order.time.isNotBlank()) append(order.time)
            append(" · ${order.orderType.name}")
            if (order.headcount > 0) append(" · ${order.headcount} people")
            if (order.address.isNotBlank() && order.orderType == OrderType.DELIVERY) {
                append("\n${order.address}")
            }
        }
        val items = order.items.filter { it.isPackingItem() }.map { item ->
            val note = if (item.note.isNotBlank()) " — ${item.note}" else ""
            "${item.displayName()}$note"
        }
        val supplies = buildCondensedSupplyLines(
            items = order.items,
            plateCount = order.plateCount,
            napkinCount = order.napkinCount,
            forksNeeded = order.forksNeeded,
            utensilTally = order.tallyServingUtensils(),
            cateringBoxCount = orderCateringBoxCount(order),
        ) + supplyNoteLinesForOrder(order)
        val notes = buildList {
            if (order.note.isNotBlank()) add("Order: ${order.note}")
            order.items.filter { it.note.isNotBlank() }.forEach { item ->
                add("${item.displayName()}: ${item.note}")
            }
        }
        OrderChecklist(name, subtitle, items, supplies, notes)
    }

    return CateringPrintReport(
        generatedAt, statLine, orderSummaries, headsUp,
        supplyLines, itemSections, checklists,
    )
}

// ─── PDF rendering ────────────────────────────────────────────────────────────

private class PdfReportRenderer(private val doc: PdfDocument) {

    // ── Page geometry ──────────────────────────────────────────────────────
    private val pageWidth    = 612f
    private val pageHeight   = 792f
    private val margin       = 44f
    private val footerH      = 24f   // space reserved at the bottom for the footer line
    /** Lowest Y coordinate that normal content may touch. */
    private val contentBottom = pageHeight - margin - footerH
    private val contentWidth  = pageWidth - margin * 2

    // ── Typography constants ───────────────────────────────────────────────
    private val lineHeight   = 15f   // vertical advance per body line
    private val sectionGap   = 10f
    private val checkboxSize = 9f
    private val checkboxPad  = 8f    // gap between checkbox right edge and text

    // ── Paints ────────────────────────────────────────────────────────────
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f; isFakeBoldText = true; color = Color.BLACK
    }
    private val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f; isFakeBoldText = true; color = Color.BLACK
    }
    private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f; isFakeBoldText = true; color = Color.BLACK
    }
    private val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f; isFakeBoldText = true; color = Color.DKGRAY
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f; color = Color.BLACK
    }
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9.5f; color = Color.DKGRAY
    }
    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f; color = Color.GRAY
    }
    private val dashRulePaint = Paint().apply {
        color = Color.rgb(160, 160, 160); strokeWidth = 0.75f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(4f, 3f), 0f)
    }
    private val rulePaint = Paint().apply {
        color = Color.LTGRAY; strokeWidth = 0.75f
    }
    private val checkboxBorder = Paint().apply {
        color = Color.rgb(120, 120, 120)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    // ── State ─────────────────────────────────────────────────────────────
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = margin
    private var pageNumber = 0

    // ─── Page management ──────────────────────────────────────────────────

    fun finish() { finishCurrentPage() }

    private fun finishCurrentPage() {
        page?.let { p ->
            // Footer: thin rule then text
            val fy = pageHeight - margin / 2f - 4f
            canvas!!.drawLine(margin, fy - 10f, pageWidth - margin, fy - 10f, rulePaint)
            canvas!!.drawText(
                "Page $pageNumber",
                margin, fy, footerPaint,
            )
            doc.finishPage(p)
        }
        page = null
        canvas = null
    }

    private fun newPage() {
        finishCurrentPage()
        pageNumber++
        y = margin
        val info = PdfDocument.PageInfo.Builder(
            pageWidth.toInt(), pageHeight.toInt(), pageNumber,
        ).create()
        page = doc.startPage(info)
        canvas = page!!.canvas
    }

    private fun startPageIfNeeded() {
        if (page == null) newPage()
    }

    /**
     * Ensure [needed] vertical pixels are available below [y].
     * Opens a new page if not. Always guarantees a live page after the call.
     */
    private fun ensureSpace(needed: Float) {
        startPageIfNeeded()
        if (y + needed > contentBottom) newPage()
    }

    /**
     * Before a major section, break to a new page only when less than
     * [minRemaining] pixels remain — enough to not orphan a banner + 1-2
     * lines of content, but no more. A large value here causes blank
     * half-pages, so keep it tight (~80 px = banner + ~3 lines).
     */
    private fun ensureMinRemaining(minRemaining: Float) {
        startPageIfNeeded()
        if (!atPageTop && (contentBottom - y) < minRemaining) newPage()
    }

    /** True when y is at the very top of a freshly opened page. */
    private val atPageTop: Boolean get() = y <= margin + 4f

    // ─── Text utilities ───────────────────────────────────────────────────

    private fun wrapText(text: String, paint: Paint, width: Float): List<String> {
        if (text.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var current = StringBuilder()
        for (word in words) {
            val chunks = breakLongToken(word, paint, width)
            for (chunk in chunks) {
                val candidate = if (current.isEmpty()) chunk else "$current $chunk"
                if (paint.measureText(candidate) <= width) {
                    current = StringBuilder(candidate)
                } else {
                    if (current.isNotEmpty()) lines.add(current.toString())
                    current = when {
                        paint.measureText(chunk) <= width -> StringBuilder(chunk)
                        else -> { lines.add(chunk); StringBuilder() }
                    }
                }
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf(text) }
    }

    private fun breakLongToken(token: String, paint: Paint, width: Float): List<String> {
        if (paint.measureText(token) <= width) return listOf(token)
        val parts = mutableListOf<String>()
        var start = 0
        while (start < token.length) {
            var end = start + 1
            while (end <= token.length && paint.measureText(token.substring(start, end)) <= width) end++
            if (end - 1 == start) end = start + 1
            parts.add(token.substring(start, end - 1))
            start = end - 1
        }
        return parts
    }

    /**
     * Measure how many vertical pixels a list of text lines will consume,
     * including optional checkbox indentation.
     */
    private fun measureLines(
        lines: List<String>,
        paint: Paint,
        indent: Float = 0f,
        checkbox: Boolean = false,
    ): Float {
        val textW = contentWidth - indent - if (checkbox) checkboxSize + checkboxPad else 0f
        var h = 0f
        lines.forEach { line ->
            h += wrapText(line, paint, textW).size * lineHeight
        }
        return h
    }

    // ─── Drawing primitives ───────────────────────────────────────────────

    private fun drawCheckbox(x: Float, top: Float) {
        canvas!!.drawRect(x, top, x + checkboxSize, top + checkboxSize, checkboxBorder)
    }

    private fun drawHRule() {
        canvas!!.drawLine(margin, y, pageWidth - margin, y, rulePaint)
    }

    private fun drawWrappedLines(
        lines: List<String>,
        paint: Paint,
        indent: Float = 0f,
        checkbox: Boolean = false,
    ) {
        val textIndent = indent + if (checkbox) checkboxSize + checkboxPad else 0f
        val textW = contentWidth - textIndent
        lines.forEach { line ->
            wrapText(line, paint, textW).forEach { wrapped ->
                ensureSpace(lineHeight)
                if (checkbox) drawCheckbox(margin + indent, y + 1f)
                canvas!!.drawText(wrapped, margin + textIndent, y + 11f, paint)
                y += lineHeight
            }
        }
    }

    private fun drawLabel(text: String) {
        ensureSpace(18f)
        canvas!!.drawText(text, margin, y + 10f, subHeaderPaint)
        y += 14f
    }

    private fun drawSpacer(height: Float) { y += height }

    private fun drawDocTitle(title: String, subtitle: String, stats: String) {
        startPageIfNeeded()
        ensureSpace(62f)
        canvas!!.drawText(title, margin, y + 18f, titlePaint)
        y += 26f
        canvas!!.drawText(subtitle, margin, y + 9f, dimPaint)
        y += 14f
        canvas!!.drawText(stats, margin, y + 9f, sectionPaint)
        y += 14f
        drawHRule()
        y += 12f
    }

    private fun drawSectionBanner(title: String, subtitle: String) {
        // ~56px = dash rule (6) + title line (16) + subtitle line (14) + gap (8) + dash rule (6) + gap (6)
        ensureMinRemaining(80f)
        // Top dashes
        canvas!!.drawLine(margin, y + 4f, pageWidth - margin, y + 4f, dashRulePaint)
        y += 10f
        canvas!!.drawText(title, margin, y + 11f, bannerPaint)
        y += 15f
        canvas!!.drawText(subtitle, margin, y + 9f, dimPaint)
        y += 13f
        // Bottom dashes
        canvas!!.drawLine(margin, y + 2f, pageWidth - margin, y + 2f, dashRulePaint)
        y += 10f
    }

    // ─── Main render ──────────────────────────────────────────────────────

    fun render(report: CateringPrintReport) {
        drawDocTitle("Catering", report.generatedAt, report.statLine)
        drawSpacer(6f)

        drawLabel("TOMORROW'S ORDERS")
        drawWrappedLines(report.orderSummaries, bodyPaint)
        drawSpacer(sectionGap)

        drawLabel("HEADS UP")
        drawWrappedLines(
            report.headsUpLines.ifEmpty { listOf("Nothing flagged.") },
            bodyPaint,
        )

        // ── Combined totals section ──────────────────────────────────────
        if (report.allItemSections.isNotEmpty() || report.allSupplyLines.isNotEmpty()) {
            drawSpacer(sectionGap)
            drawSectionBanner("COMBINED TOTALS", "Everything needed across all orders")
            drawSpacer(4f)

            report.allItemSections.forEach { block ->
                drawLabel(block.header)
                drawWrappedLines(block.lines, bodyPaint)
                drawSpacer(6f)
            }

            drawLabel("ALL SUPPLIES")
            drawWrappedLines(
                report.allSupplyLines.ifEmpty { listOf("No supplies calculated.") },
                bodyPaint,
            )
        }

        // ── Per-order checklists — each customer gets their own page ─────
        if (report.orderChecklists.isNotEmpty()) {
            report.orderChecklists.forEach { checklist ->
                // Always start a fresh page for each customer checklist
                newPage()
                drawOrderChecklist(checklist)
            }
        }
    }

    // ─── Order checklist ─────────────────────────────────────────────────

    private fun drawOrderChecklist(checklist: OrderChecklist) {
        // We always land here at the top of a fresh page (called from render),
        // so no keep-together estimation needed. Add a small top margin.
        y += 6f

        var continued = false

        // ── Draws the order header (name, subtitle, separator) ────────────
        fun drawOrderHeader() {
            val subtitleLines = checklist.subtitle.split("\n")
            val headerH = 16f + (subtitleLines.size * lineHeight) + 4f + 1f + 8f
            ensureSpace(headerH)

            val nameText = if (continued)
                "${checklist.title.uppercase()} (continued)"
            else
                checklist.title.uppercase()
            canvas!!.drawText(nameText, margin, y + 11f, sectionPaint)
            y += 16f

            subtitleLines.forEach { line ->
                ensureSpace(lineHeight)
                canvas!!.drawText(line, margin, y + 10f, dimPaint)
                y += lineHeight
            }
            y += 4f
            drawHRule()
            y += 8f
        }

        // ── Sub-section label; keeps label + 2 body lines together ───────
        fun drawSubLabel(label: String) {
            val minTogether = 16f + lineHeight * 2
            if (y + minTogether > contentBottom) {
                newPage()
                continued = true
                drawOrderHeader()
            }
            // Space is now guaranteed — draw directly, no second ensureSpace
            canvas!!.drawText(label, margin, y + 10f, sectionPaint)
            y += 16f
        }

        // ── Checkbox lines, handling page breaks ──────────────────────────
        fun drawCheckboxLines(lines: List<String>, continueLabel: String) {
            val checkW = contentWidth - checkboxSize - checkboxPad
            lines.forEach { line ->
                wrapText(line, bodyPaint, checkW).forEach { wrapped ->
                    if (y + lineHeight > contentBottom) {
                        newPage()
                        continued = true
                        drawOrderHeader()
                        canvas!!.drawText(continueLabel, margin, y + 10f, sectionPaint)
                        y += 16f
                    }
                    drawCheckbox(margin, y + 1f)
                    canvas!!.drawText(wrapped, margin + checkboxSize + checkboxPad, y + 11f, bodyPaint)
                    y += lineHeight
                }
            }
        }

        // ── Render ────────────────────────────────────────────────────────
        drawOrderHeader()

        if (checklist.itemLines.isNotEmpty()) {
            drawSubLabel("ITEMS TO PACK")
            drawCheckboxLines(checklist.itemLines, "ITEMS TO PACK (cont.)")
        }

        if (checklist.supplyLines.isNotEmpty()) {
            drawSpacer(6f)
            drawSubLabel("SUPPLIES")
            drawCheckboxLines(checklist.supplyLines, "SUPPLIES (cont.)")
        }

        if (checklist.noteLines.isNotEmpty()) {
            drawSpacer(6f)
            drawSubLabel("NOTES")
            checklist.noteLines.forEach { note ->
                wrapText("• $note", bodyPaint, contentWidth - 8f).forEach { wrapped ->
                    ensureSpace(lineHeight)
                    canvas!!.drawText(wrapped, margin + 8f, y + 11f, bodyPaint)
                    y += lineHeight
                }
            }
        }

        // Bottom separator
        drawSpacer(6f)
        if (!atPageTop) {
            drawHRule()
            y += 8f
        }
    }
}

// ─── Public API ───────────────────────────────────────────────────────────────

fun generateCateringReportPdf(context: Context, orders: List<CateringOrder>): File {
    val report = buildCateringPrintReport(orders)
    val doc = PdfDocument()
    PdfReportRenderer(doc).apply {
        render(report)
        finish()
    }
    val file = File(context.cacheDir, "catering_report_${System.currentTimeMillis()}.pdf")
    FileOutputStream(file).use { doc.writeTo(it) }
    doc.close()
    return file
}

fun launchCateringReportEmail(context: Context, orders: List<CateringOrder>, toEmail: String) {
    if (orders.isEmpty()) return
    val file = generateCateringReportPdf(context, orders)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val dateLabel = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))   // ← was hardcoded
        putExtra(Intent.EXTRA_SUBJECT, "Catering Print Packet — $dateLabel")
        putExtra(
            Intent.EXTRA_TEXT,
            "Attached PDF: overview, combined totals, and per-order checklists for tomorrow's catering.",
        )
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Email catering report"))
}

private class CateringPdfPrintAdapter(
    private val pdfFile: File,
    private val jobName: String,
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?,
    ) {
        if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pdfPageCount(pdfFile))
            .build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        if (cancellationSignal.isCanceled) { callback.onWriteCancelled(); return }
        try {
            FileInputStream(pdfFile).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}

private fun pdfPageCount(file: File): Int {
    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(pfd)
    val count = renderer.pageCount
    renderer.close()
    pfd.close()
    return count
}

fun printCateringReport(context: Context, orders: List<CateringOrder>) {
    if (orders.isEmpty()) return
    val activity = context as? Activity ?: return
    val file = generateCateringReportPdf(context, orders)
    val dateLabel = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
    val jobName = "Catering Print Packet — $dateLabel"
    val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
    printManager.print(
        jobName,
        CateringPdfPrintAdapter(file, jobName),
        PrintAttributes.Builder().build(),
    )
}