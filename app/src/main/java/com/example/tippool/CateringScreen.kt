package com.tippool.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippool.*
import com.tippool.ui.theme.*

// ─── Root screen ──────────────────────────────────────────────────────────────

@Composable
fun CateringScreen(vm: CateringViewModel) {

    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.load(context) }

    var settings by remember { mutableStateOf(loadSettings(context)) }
    var showLocationPicker by remember { mutableStateOf(settings.locationEmail == null) }

    if (showLocationPicker) {
        AlertDialog(
            onDismissRequest = { /* force a choice — don't dismiss */ },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Which location?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Sets the catering report email. Changeable later in Settings.", color = TextDim, fontSize = 12.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LocationChoiceButton("Argonne", "location-a@example.com", Orange) { email ->
                        val updated = settings.copy(locationEmail = email)
                        save(context, updated)
                        settings = updated
                        showLocationPicker = false
                    }
                    LocationChoiceButton("CDA", "location-b@example.com", Blue) { email ->
                        val updated = settings.copy(locationEmail = email)
                        save(context, updated)
                        settings = updated
                        showLocationPicker = false
                    }
                }
            },
            confirmButton = {},
        )
    }





    var showAddOrder   by remember { mutableStateOf(false) }
    var editingOrder   by remember { mutableStateOf<CateringOrder?>(null) }
    var showSupplyList by remember { mutableStateOf(false) }
    var showSavedDays  by remember { mutableStateOf(false) }
    var supplyByOrder  by remember { mutableStateOf(false) }
    var copiedToast    by remember { mutableStateOf(false) }
    var savedDayToast  by remember { mutableStateOf<String?>(null) }
    var duplicateReview by remember { mutableStateOf<SaveDayResult?>(null) }
    var duplicateReviewImported by remember { mutableStateOf<SaveDayResult?>(null) }
    var duplicateReviewCurrent by remember { mutableStateOf<List<List<CateringOrder>>?>(null) }

    LaunchedEffect(copiedToast) { if (copiedToast) { kotlinx.coroutines.delay(2000); copiedToast = false } }
    LaunchedEffect(savedDayToast) { if (savedDayToast != null) { kotlinx.coroutines.delay(2500); savedDayToast = null } }

    val onSubScreenBack: () -> Unit = {
        when {
            showSavedDays -> showSavedDays = false
            showSupplyList -> showSupplyList = false
            showAddOrder || editingOrder != null -> {
                clearDraftOrder(context)
                showAddOrder = false; editingOrder = null
            }
        }
    }
    BackHandler(enabled = showSavedDays || showSupplyList || showAddOrder || editingOrder != null, onBack = onSubScreenBack)

    if (showAddOrder || editingOrder != null) {
        OrderEditScreen(
            existing = editingOrder,
            onSave   = { order -> if (editingOrder != null) vm.updateOrder(context, order) else vm.addOrder(context, order); showAddOrder = false; editingOrder = null },
            onCancel = { showAddOrder = false; editingOrder = null },
        ); return
    }

    if (showSupplyList) {
        SupplyListScreen(vm = vm, byOrder = supplyByOrder, onToggle = { supplyByOrder = !supplyByOrder }, onBack = { showSupplyList = false }); return
    }

    if (showSavedDays) {
        SavedDaysScreen(
            context = context,
            vm = vm,
            onOpen = { key -> vm.openSavedDay(context, key); showSavedDays = false },
            onImportedSavedDay = { result ->
                vm.openSavedDay(context, result.dateKey)
                showSavedDays = false
                if (result.similarGroups.isNotEmpty()) duplicateReviewImported = result
                else savedDayToast = "Imported into ${formatDayKeyForDisplay(result.dateKey)}"
            },
            onImportedCurrentCatering = { groups ->
                vm.returnToCurrentCatering(context)
                showSavedDays = false
                if (groups.isNotEmpty()) duplicateReviewCurrent = groups
                else savedDayToast = "Imported into Current Catering"
            },
            onBack = { showSavedDays = false },
        ); return
    }

    Column(modifier = Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(8.dp).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Blue))
                Column {
                    Text("Catering", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Text(
                        if (vm.activeDayKey == null) "CURRENT CATERING" else formatDayKeyForDisplay(vm.activeDayKey!!).uppercase(java.util.Locale.US),
                        color = TextDim, fontSize = 11.sp, letterSpacing = 2.sp,
                    )
                }
            }
            if (vm.orders.isNotEmpty()) {
                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(10.dp)).clickable { showSupplyList = true }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("📋 Supply List", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(10.dp))
                    .clickable { showSavedDays = true }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Text("📅 Days", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium) }

            if (vm.activeDayKey == null) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(10.dp))
                        .clickable {
                            showDatePicker(context) { dateKey ->
                                val result = vm.saveCurrentCateringToDay(context, dateKey)
                                if (result.similarGroups.isNotEmpty()) duplicateReview = result
                                else savedDayToast = "Archived a copy under ${formatDayKeyForDisplay(dateKey)}"
                            }
                        }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("💾 Save", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            } else {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(10.dp))
                        .clickable { vm.returnToCurrentCatering(context) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("← Current", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            }

            if (vm.orders.isNotEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(10.dp))
                        .clickable {
                            val label = vm.activeDayKey?.let { formatDayKeyForDisplay(it) } ?: "Current Catering"
                            shareTextViaSheet(context, exportDayToJson(vm.activeDayKey, vm.orders), "Catering: $label")
                        }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("📤 Export", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            }
        }

        if (vm.activeDayKey != null) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ExtrasBg).border(1.5.dp, ExtrasBorder, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("Editing saved day — changes save automatically", color = ExtrasText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        savedDayToast?.let {
            Text("✓ $it", color = Green, fontSize = 12.sp)
        }

        if (vm.orders.isNotEmpty()) {
            CateringCrewActions(
                copiedHandoff = copiedToast,
                onCopyHandoff = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Catering Handoff", vm.generateHandoffText()))
                    copiedToast = true
                },
                onEmail = { launchCateringReportEmail(context, vm.orders, settings.resolvedCateringEmail()) },
                onPrint = { printCateringReport(context, vm.orders) },
            )
        }

        if (vm.orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).border(1.5.dp, Border, RoundedCornerShape(16.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No orders yet", color = TextMuted, fontSize = 16.sp); Text("Tap + to add tomorrow's first catering order", color = TextDim, fontSize = 13.sp)
                }
            }
        } else { vm.orders.forEach { order -> OrderCard(order = order, onEdit = { editingOrder = order }, onDelete = { vm.deleteOrder(context, order.id) }) } }

        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Blue).clickable { showAddOrder = true }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            Text("+ Add Order", color = Background, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (vm.orders.isNotEmpty()) {
            var showConfirm by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceDark).border(1.5.dp, ErrorBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).clickable { showConfirm = true }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text("Clear All Orders", color = ErrorText, fontSize = 14.sp) }
            if (showConfirm) AlertDialog(onDismissRequest = { showConfirm = false }, title = { Text("Clear all orders?", color = TextPrimary) }, text = { Text("This deletes all ${vm.orders.size} orders.", color = TextSecondary) }, confirmButton = { TextButton(onClick = { vm.clearAll(context); showConfirm = false }) { Text("Clear All", color = ErrorText) } }, dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = TextMuted) } }, containerColor = Surface)
        }
        Spacer(Modifier.height(16.dp))
    }

    duplicateReview?.let { result ->
        DuplicateNamesDialog(
            dateLabel = formatDayKeyForDisplay(result.dateKey),
            groups = result.similarGroups,
            onDone = { toDelete ->
                deleteOrdersFromSavedDay(context, result.dateKey, toDelete)
                savedDayToast = "Archived a copy under ${formatDayKeyForDisplay(result.dateKey)}"
                duplicateReview = null
            },
        )
    }

    duplicateReviewImported?.let { result ->
        DuplicateNamesDialog(
            dateLabel = formatDayKeyForDisplay(result.dateKey),
            groups = result.similarGroups,
            onDone = { toDelete ->
                deleteOrdersFromSavedDay(context, result.dateKey, toDelete)
                savedDayToast = "Imported into ${formatDayKeyForDisplay(result.dateKey)}"
                duplicateReviewImported = null
            },
        )
    }

    duplicateReviewCurrent?.let { groups ->
        DuplicateNamesDialog(
            dateLabel = "Current Catering",
            groups = groups,
            onDone = { toDelete ->
                vm.deleteOrdersFromCurrentCatering(context, toDelete)
                savedDayToast = "Imported into Current Catering"
                duplicateReviewCurrent = null
            },
        )
    }
}

// ─── Saved days ─────────────────────────────────────────────────────────────

private fun showDatePicker(context: Context, initialDateKey: String? = null, onPicked: (String) -> Unit) {
    val cal = java.util.Calendar.getInstance()
    if (initialDateKey != null) {
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(initialDateKey)?.let { cal.time = it }
        } catch (e: Exception) { /* fall back to today */ }
    }
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val picked = java.util.Calendar.getInstance()
            picked.set(year, month, dayOfMonth)
            onPicked(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(picked.time))
        },
        cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH),
    ).show()
}

private fun shareTextViaSheet(context: Context, text: String, subject: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share catering day"))
}

@Composable
private fun SavedDaysScreen(
    context: Context,
    vm: CateringViewModel,
    onOpen: (String) -> Unit,
    onImportedSavedDay: (SaveDayResult) -> Unit,
    onImportedCurrentCatering: (List<List<CateringOrder>>) -> Unit,
    onBack: () -> Unit,
) {
    var days by remember { mutableStateOf(listSavedDayKeys(context).map { it to loadSavedDay(context, it).size }) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<ImportedDay?>(null) }

    fun refreshDays() { days = listSavedDayKeys(context).map { it to loadSavedDay(context, it).size } }

    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(8.dp).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Blue))
                Text("Saved Days", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            SmallBtn("Back", TextMuted, onBack)
        }

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(10.dp))
                .clickable { showImport = true }.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text("📥 Import Day", color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium) }

        if (days.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).border(1.5.dp, Border, RoundedCornerShape(16.dp)).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No saved days yet", color = TextMuted, fontSize = 15.sp)
                    Text("Use \"Save\" on Current Catering to archive a day here", color = TextDim, fontSize = 12.sp)
                }
            }
        } else {
            days.forEach { (key, count) ->
                SectionCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).clickable { onOpen(key) }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(formatDayKeyForDisplay(key), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("$count order${if (count != 1) "s" else ""}", color = TextDim, fontSize = 12.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SmallBtn("Open", Blue) { onOpen(key) }
                            SmallBtn("Export", TextMuted) {
                                shareTextViaSheet(context, exportDayToJson(key, loadSavedDay(context, key)), "Catering: ${formatDayKeyForDisplay(key)}")
                            }
                            SmallBtn("Delete", ErrorText) { pendingDelete = key }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { key ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Surface,
            title = { Text("Delete this saved day?", color = TextPrimary) },
            text = { Text("This permanently deletes ${formatDayKeyForDisplay(key)} and all its orders.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    deleteSavedDay(context, key)
                    refreshDays()
                    pendingDelete = null
                }) { Text("Delete", color = ErrorText) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = TextMuted) } },
        )
    }

    if (showImport) {
        ImportDayDialog(
            context = context,
            onImport = { imported -> showImport = false; pendingImport = imported },
            onDismiss = { showImport = false },
        )
    }

    // Picking a target immediately merges and navigates there — no extra "Open" tap needed.
    pendingImport?.let { imported ->
        val n = imported.orders.size
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Import $n order${if (n != 1) "s" else ""} into…", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(10.dp))
                            .clickable {
                                pendingImport = null
                                val (_, groups) = vm.importIntoCurrentCatering(context, imported.orders)
                                onImportedCurrentCatering(groups)
                            }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Current Catering", color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(10.dp))
                            .clickable {
                                pendingImport = null
                                showDatePicker(context, initialDateKey = imported.dateKey) { dateKey ->
                                    val result = mergeOrdersIntoDay(context, dateKey, imported.orders)
                                    onImportedSavedDay(result)
                                }
                            }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Saved Day (pick date)", color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                    Box(modifier = Modifier.fillMaxWidth().clickable { pendingImport = null }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text("Cancel", color = TextMuted, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun ImportDayDialog(context: Context, onImport: (ImportedDay) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Import a Day", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Paste the text a shift lead exported to you, then pick which date to file it under.", color = TextDim, fontSize = 12.sp)
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceDark)
                        .border(1.5.dp, if (error != null) ErrorBorder else BorderLight, RoundedCornerShape(10.dp)).padding(10.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it; error = null },
                        textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(Blue),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner -> if (text.isEmpty()) Text("Paste exported text here…", color = TextDim, fontSize = 12.sp); inner() },
                    )
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceDark).border(1.dp, BorderLight, RoundedCornerShape(8.dp))
                        .clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.primaryClip?.let { clip -> if (clip.itemCount > 0) text = clip.getItemAt(0).text?.toString() ?: text }
                            error = null
                        }.padding(horizontal = 12.dp, vertical = 8.dp),
                ) { Text("Paste from clipboard", color = Blue, fontSize = 12.sp) }
                error?.let { Text(it, color = ErrorText, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = parseExportedDayJson(text)
                if (parsed == null) error = "Couldn't read that — check it's the exported text and try again."
                else onImport(parsed)
            }) { Text("Import", color = Blue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } },
    )
}

@Composable
private fun DuplicateNamesDialog(dateLabel: String, groups: List<List<CateringOrder>>, onDone: (Set<String>) -> Unit) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = { onDone(emptySet()) },
        containerColor = Surface,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Possible duplicates in $dateLabel", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("These orders have very similar customer names. Check any you'd like to delete.", color = TextDim, fontSize = 12.sp)
                groups.forEach { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        group.forEach { order ->
                            val checked = order.id in selected
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selected = if (checked) selected - order.id else selected + order.id },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))
                                        .background(if (checked) ErrorText else SurfaceDark)
                                        .border(1.5.dp, if (checked) ErrorText else BorderLight, RoundedCornerShape(5.dp)),
                                )
                                Column {
                                    Text(order.customerName.ifBlank { "Unnamed order" }, color = TextPrimary, fontSize = 14.sp)
                                    Text(order.time.ifBlank { "no time set" }, color = TextDim, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (selected.isNotEmpty()) onDone(selected) }, enabled = selected.isNotEmpty()) {
                Text("Delete selected (${selected.size})", color = if (selected.isNotEmpty()) ErrorText else TextDim)
            }
        },
        dismissButton = { TextButton(onClick = { onDone(emptySet()) }) { Text("Keep all", color = TextMuted) } },
    )
}

// ─── Order card ───────────────────────────────────────────────────────────────

@Composable
private fun OrderCard(order: CateringOrder, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded   by remember { mutableStateOf(true) }
    var showDelete by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).border(1.5.dp, Border, RoundedCornerShape(16.dp))) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(order.customerName.ifBlank { "Unnamed Order" }, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold); TypeBadge(order.orderType)
                    }
                    if (order.time.isNotBlank()) Text(order.time, color = TextMuted, fontSize = 12.sp)
                    if (order.address.isNotBlank()) Text(order.address, color = TextDim, fontSize = 11.sp)
                    Text("${order.items.size} item${if (order.items.size != 1) "s" else ""}${if (order.headcount > 0) " · ${order.headcount} people" else ""}", color = TextDim, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SmallBtn("Edit", Blue) { onEdit() }; SmallBtn("✕", ErrorText) { showDelete = true }
                    Text(if (expanded) "▲" else "▼", color = TextDim, fontSize = 10.sp)
                }
            }
            if (expanded) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.note.isNotBlank()) Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceDark).border(1.dp, BorderLight, RoundedCornerShape(8.dp)).padding(10.dp)) { Text("📝 ${order.note}${noteVisibilitySuffix(order.noteInHandoff, order.noteInSupplyList)}", color = TextSecondary, fontSize = 13.sp) }
                    order.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(Blue.copy(alpha = 0.5f)))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.displayName(), color = TextPrimary, fontSize = 14.sp)
                                if (item.note.isNotBlank()) Text("↳ ${item.note}${noteVisibilitySuffix(item.noteInHandoff, item.noteInSupplyList)}", color = TextDim, fontSize = 12.sp)
                            }
                        }
                    }
                    if (order.plateCount > 0 || order.napkinCount > 0) {
                        plateNapkinSupplyLines(order.plateCount, order.napkinCount).forEach { line ->
                            Text("→ $line", color = TextDim, fontSize = 12.sp)
                        }
                    }
                    if (order.forksNeeded  > 0) Text("→ ${order.forksNeeded} forks", color = TextDim, fontSize = 12.sp)
                    order.tallyServingUtensils().summaryOrNull()?.let { Text("→ $it", color = TextDim, fontSize = 12.sp) }
                }
            }
        }
    }
    if (showDelete) AlertDialog(onDismissRequest = { showDelete = false }, title = { Text("Delete order?", color = TextPrimary) }, text = { Text("Remove ${order.customerName.ifBlank { "this order" }}?", color = TextSecondary) }, confirmButton = { TextButton(onClick = { onDelete(); showDelete = false }) { Text("Delete", color = ErrorText) } }, dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel", color = TextMuted) } }, containerColor = Surface)
}

@Composable
private fun SupplyListLine(line: String) {
    val isSub = line.startsWith("  →")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = if (isSub) 12.dp else 0.dp),
    ) {
        if (!isSub) {
            Box(modifier = Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(Blue.copy(alpha = 0.5f)))
        }
        Text(
            if (isSub) line.trimStart() else line,
            color = if (isSub) TextDim else TextSecondary,
            fontSize = if (isSub) 13.sp else 14.sp,
        )
    }
}

@Composable private fun TypeBadge(type: OrderType) {
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (type == OrderType.DELIVERY) Orange.copy(alpha = 0.15f) else Blue.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(type.name, color = if (type == OrderType.DELIVERY) Orange else Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// ─── Supply list screen ───────────────────────────────────────────────────────

@Composable
private fun SupplyListScreen(vm: CateringViewModel, byOrder: Boolean, onToggle: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var copiedList by remember { mutableStateOf(false) }
    LaunchedEffect(copiedList) { if (copiedList) { kotlinx.coroutines.delay(2000); copiedList = false } }

    Column(modifier = Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(8.dp).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Blue))
                Column { Text("Supply List", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp); Text(if (byOrder) "BY ORDER" else "ALL COMBINED", color = TextDim, fontSize = 11.sp, letterSpacing = 2.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallBtn(if (byOrder) "Combined" else "By Order", Blue) { onToggle() }
                SmallBtn("← Back", TextMuted) { onBack() }
            }
        }

        // Copy supply list button
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.5.dp, Blue.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).clickable {
            val text = buildString {
                if (byOrder) {
                    vm.perOrderSupplyLines().forEach { result ->
                        appendLine("=== ${result.order.customerName.ifBlank { "Unnamed" }} · ${result.order.time} ===")
                        result.lines.forEach { appendLine(it) }
                        appendLine()
                    }
                } else {
                    vm.aggregatedSupplyLines().forEach { appendLine(it) }
                }
            }
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Supply List", text)); copiedList = true
        }.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📋", fontSize = 16.sp)
                Text(if (copiedList) "Copied! ✓" else "Copy supply list", color = if (copiedList) Green else Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (byOrder) {
            vm.perOrderSupplyLines().forEach { result ->
                SectionCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(result.order.customerName.ifBlank { "Unnamed Order" }, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            if (result.order.time.isNotBlank()) Text("${result.order.time} · ${result.order.orderType.name}", color = TextMuted, fontSize = 12.sp)
                            if (result.order.address.isNotBlank()) Text(result.order.address, color = TextDim, fontSize = 11.sp)
                        }
                        TypeBadge(result.order.orderType)
                    }
                    Spacer(Modifier.height(10.dp)); Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border)); Spacer(Modifier.height(10.dp))

                    if (result.lines.isEmpty()) Text("No items", color = TextDim, fontSize = 13.sp)
                    else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.lines.forEach { line -> SupplyListLine(line) }
                    }
                }
            }
        } else {
            SectionCard {
                SectionLabel("Everything to grab")
                val lines = vm.aggregatedSupplyLines()
                if (lines.isEmpty()) Text("Nothing to grab yet", color = TextDim, fontSize = 13.sp)
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lines.forEach { line -> SupplyListLine(line) }
                }
            }
            Text("Tap \"By Order\" to see items split per customer", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ─── Order edit screen ────────────────────────────────────────────────────────

@Composable
private fun OrderEditScreen(existing: CateringOrder?, onSave: (CateringOrder) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val draftId = existing?.id ?: ""

    var name         by remember { mutableStateOf(existing?.customerName ?: "") }
    var time         by remember { mutableStateOf(existing?.time ?: "") }
    var address      by remember { mutableStateOf(existing?.address ?: "") }
    var orderType    by remember { mutableStateOf(existing?.orderType ?: OrderType.PICKUP) }
    var headcount    by remember { mutableStateOf(if ((existing?.headcount ?: 0) > 0) existing!!.headcount.toString() else "") }
    var needsPlates  by remember { mutableStateOf(existing?.needsPlates ?: false) }
    var needsNapkins by remember { mutableStateOf(existing?.needsNapkins ?: false) }
    var needsForks   by remember { mutableStateOf(existing?.needsForks ?: false) }
    var needsServingUtensils by remember { mutableStateOf(existing?.needsServingUtensils ?: false) }
    var orderNote    by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff  by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply   by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var items        by remember { mutableStateOf(existing?.items ?: emptyList()) }
    var showItemPicker by remember { mutableStateOf(false) }
    var editingItemIdx by remember { mutableStateOf<Int?>(null) }

    // Restore an in-progress draft (if one matching this order exists) before we start autosaving,
    // so a killed/closed app doesn't lose progress on a new or partially-edited order.
    var draftRestored by remember { mutableStateOf(false) }
    LaunchedEffect(draftId) {
        loadDraftOrder(context, draftId)?.let { draft ->
            name = draft.customerName
            time = draft.time
            address = draft.address
            orderType = draft.orderType
            headcount = if (draft.headcount > 0) draft.headcount.toString() else ""
            needsPlates = draft.needsPlates
            needsNapkins = draft.needsNapkins
            needsForks = draft.needsForks
            needsServingUtensils = draft.needsServingUtensils
            orderNote = draft.note
            noteHandoff = draft.noteInHandoff
            noteSupply = draft.noteInSupplyList
            items = draft.items
        }
        draftRestored = true
    }

    // Continually persist the in-progress order as a draft so closing the app early doesn't lose it.
    LaunchedEffect(draftRestored, name, time, address, orderType, headcount, needsPlates, needsNapkins, needsForks, needsServingUtensils, orderNote, noteHandoff, noteSupply, items) {
        if (!draftRestored) return@LaunchedEffect
        saveDraftOrder(
            context, draftId,
            CateringOrder(
                id = existing?.id ?: "", customerName = name, time = time, address = address,
                orderType = orderType, headcount = headcount.toIntOrNull() ?: 0,
                needsPlates = needsPlates, needsNapkins = needsNapkins,
                needsForks = needsForks, needsServingUtensils = needsServingUtensils,
                note = orderNote, noteInHandoff = noteHandoff, noteInSupplyList = noteSupply, items = items,
            ),
        )
    }

    BackHandler(enabled = showItemPicker || editingItemIdx != null) {
        showItemPicker = false
        editingItemIdx = null
    }

    if (showItemPicker || editingItemIdx != null) {
        ItemEditSheet(
            existing = editingItemIdx?.let { items[it] },
            onSave   = { item ->
                val wasEditing = editingItemIdx != null
                items = if (wasEditing) items.toMutableList().also { l -> l[editingItemIdx!!] = item } else items + item
                editingItemIdx = null
                showItemPicker = !wasEditing
            },
            onFinish = { showItemPicker = false; editingItemIdx = null },
            onCancel = { showItemPicker = false; editingItemIdx = null },
        ); return
    }

    Column(modifier = Modifier.fillMaxSize().background(Background).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(8.dp).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Blue))
                Text(if (existing != null) "Edit Order" else "New Order", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallBtn("Cancel", TextMuted) { clearDraftOrder(context); onCancel() }
                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Blue).clickable {
                    clearDraftOrder(context)
                    onSave(CateringOrder(id = existing?.id ?: java.util.UUID.randomUUID().toString(), customerName = name, time = time, address = address, orderType = orderType, headcount = headcount.toIntOrNull() ?: 0, needsPlates = needsPlates, needsNapkins = needsNapkins, needsForks = needsForks, needsServingUtensils = needsServingUtensils, note = orderNote, noteInHandoff = noteHandoff, noteInSupplyList = noteSupply, items = items))
                }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Save", color = Background, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }

        SectionCard {
            SectionLabel("Order Details")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CatTextField("Customer name", name) { name = it }
                TimeInputField(time) { time = it }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderType.values().forEach { type ->
                        val sel = orderType == type
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (sel) Blue else SurfaceDark).border(1.5.dp, if (sel) Blue else BorderLight, RoundedCornerShape(8.dp)).clickable { orderType = type }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(type.name, color = if (sel) Background else TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                if (orderType == OrderType.DELIVERY) CatTextField("Delivery address", address, minLines = 2) { address = it }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Headcount", color = TextSecondary, fontSize = 14.sp)
                    Row(modifier = Modifier.width(100.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(value = headcount, onValueChange = { headcount = it.filter { c -> c.isDigit() }.take(4) }, singleLine = true, textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(Blue), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), decorationBox = { inner -> if (headcount.isEmpty()) Text("0", color = TextDim, fontSize = 15.sp, fontFamily = FontFamily.Monospace); inner() })
                    }
                }
                val hc = headcount.toIntOrNull() ?: 0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Plates", color = TextSecondary, fontSize = 14.sp); if (needsPlates && hc > 0) Text("~${(hc * 1.1).toInt()} plates", color = TextDim, fontSize = 11.sp) }
                    Toggle(needsPlates) { needsPlates = !needsPlates }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Napkins", color = TextSecondary, fontSize = 14.sp); if (needsNapkins && hc > 0) Text("~${(hc * 1.1).toInt()} napkins", color = TextDim, fontSize = 11.sp) }
                    Toggle(needsNapkins) { needsNapkins = !needsNapkins }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Forks", color = TextSecondary, fontSize = 14.sp)
                        if (needsForks && hc > 0) Text("~${(hc * 1.1).toInt()} forks", color = TextDim, fontSize = 11.sp)
                    }
                    Toggle(needsForks) { needsForks = !needsForks }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Serving utensils", color = TextSecondary, fontSize = 14.sp)
                        if (needsServingUtensils) Text("Tongs/spoons for salads & pasta on supply list", color = TextDim, fontSize = 11.sp)
                    }
                    Toggle(needsServingUtensils) { needsServingUtensils = !needsServingUtensils }
                }
                CatTextField("Order note (optional)", orderNote, minLines = 2) { orderNote = it }
                if (orderNote.isNotBlank()) {
                    Text("Show this order note on:", color = TextDim, fontSize = 11.sp, letterSpacing = 1.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Handoff message", color = TextSecondary, fontSize = 13.sp); Toggle(noteHandoff) { noteHandoff = !noteHandoff }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Supply list", color = TextSecondary, fontSize = 13.sp); Toggle(noteSupply) { noteSupply = !noteSupply }
                    }
                }
            }
        }

        SectionCard {
            SectionLabel("Items (${items.size})")
            if (items.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                items.forEachIndexed { index, item ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.dp, BorderLight, RoundedCornerShape(10.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) { Text(item.displayName(), color = TextPrimary, fontSize = 14.sp); if (item.note.isNotBlank()) Text("↳ ${item.note}${noteVisibilitySuffix(item.noteInHandoff, item.noteInSupplyList)}", color = TextDim, fontSize = 12.sp) }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { SmallBtn("Edit", Blue) { editingItemIdx = index; showItemPicker = false }; SmallBtn("✕", ErrorText) { items = items.toMutableList().also { it.removeAt(index) } } }
                    }
                }
            } else Text("No items added yet", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceDark).border(1.dp, Blue.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).clickable { showItemPicker = true; editingItemIdx = null }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text("+ Add Items", color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ─── Item edit sheet ──────────────────────────────────────────────────────────

@Composable
private fun ItemEditSheet(existing: CateringItem?, onSave: (CateringItem) -> Unit, onFinish: () -> Unit, onCancel: () -> Unit) {
    val itemTypes = listOf("Sandwiches", "Salad", "Wraps", "Box Lunch", "Pasta", "Pizza", "Individual", "Dessert", "Extras")
    var selectedType by remember { mutableStateOf(when (existing) { is CateringItem.Sandwiches -> "Sandwiches"; is CateringItem.Salad -> "Salad"; is CateringItem.Wraps -> "Wraps"; is CateringItem.BoxLunch -> "Box Lunch"; is CateringItem.PastaTray -> "Pasta"; is CateringItem.Pizza -> "Pizza"; is CateringItem.IndividualItems -> "Individual"; is CateringItem.Dessert -> "Dessert"; is CateringItem.Extra -> "Extras"; else -> "Sandwiches" }) }
    var lastSavedName by remember { mutableStateOf<String?>(null) }
    var showSavedToast by remember { mutableStateOf(false) }

    LaunchedEffect(showSavedToast) { if (showSavedToast) { kotlinx.coroutines.delay(1800); showSavedToast = false } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Background).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (existing != null) "Edit Item" else "Add Items", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallBtn("Cancel", TextMuted) { onCancel() }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Green.copy(alpha = 0.15f)).border(1.dp, Green.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { onFinish() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("✓ Done Adding", color = Green, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Note: Old inline toast banner block is completely removed from here!

            SectionCard {
                SectionLabel("Item type")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemTypes.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { type ->
                                val sel = selectedType == type
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (sel) Blue else SurfaceDark).border(1.5.dp, if (sel) Blue else BorderLight, RoundedCornerShape(8.dp)).clickable { selectedType = type }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(type, color = if (sel) Background else TextMuted, fontSize = 12.sp)
                                }
                            }
                            if (row.size < 3) repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            val wrappedSave: (CateringItem) -> Unit = { item ->
                lastSavedName = item.displayName().take(40)
                showSavedToast = true
                onSave(item)
            }

            key(selectedType) {
                when (selectedType) {
                    "Sandwiches" -> SandwichesForm(existing as? CateringItem.Sandwiches, wrappedSave)
                    "Wraps" -> WrapsForm(existing as? CateringItem.Wraps, wrappedSave)
                    "Salad" -> SaladForm(existing as? CateringItem.Salad, wrappedSave)
                    "Pasta" -> PastaForm(existing as? CateringItem.PastaTray, wrappedSave)
                    "Box Lunch" -> BoxLunchForm(existing as? CateringItem.BoxLunch, wrappedSave)
                    "Pizza" -> PizzaForm(existing as? CateringItem.Pizza, wrappedSave)
                    "Dessert" -> DessertForm(existing as? CateringItem.Dessert, wrappedSave)
                    "Extras" -> ExtrasForm(existing as? CateringItem.Extra, wrappedSave)
                    "Individual" -> IndividualItemsForm(existing as? CateringItem.IndividualItems, wrappedSave)
                }
            }
        }

        // Smooth Floating Toast Overlay — Pinned securely at the bottom center of the screen
        AnimatedVisibility(
            visible = showSavedToast && lastSavedName != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Green.copy(alpha = 0.95f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✓", color = Background, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${lastSavedName ?: ""} added", color = Background, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
// ─── Item forms ───────────────────────────────────────────────────────────────

@Composable
private fun SandwichesForm(existing: CateringItem.Sandwiches?, onSave: (CateringItem) -> Unit) {
    var trayType    by remember { mutableStateOf(existing?.type ?: SandwichTrayType.CLASSIC) }
    var portion     by remember { mutableStateOf(existing?.portionSize ?: SandwichPortionSize.THIRD) }
    var count       by remember { mutableStateOf(existing?.sandwichCount?.toString() ?: "") }
    var condiments  by remember { mutableStateOf(existing?.condimentsOnSide ?: false) }
    var note        by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply  by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var formError   by remember { mutableStateOf<String?>(null) }
    SectionCard {
        SectionLabel("Sandwiches")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatChipRow("Type", SandwichTrayType.values().map { it.label }, SandwichTrayType.values().indexOf(trayType)) { trayType = SandwichTrayType.values()[it] }
            CatChipRow("Portion size", SandwichPortionSize.values().map { it.label }, SandwichPortionSize.values().indexOf(portion)) { portion = SandwichPortionSize.values()[it] }
            CatNumberField("Number of sandwiches", count) { count = it; formError = null }
            val n = count.toIntOrNull() ?: 0
            if (n > 0) Text("→ ${portion.perBox} per catering box (box count calculated per order total)", color = Blue, fontSize = 13.sp)
            CatToggleRow("Mayo & mustard on the side", condiments) { condiments = it }
            NoteField(note, noteHandoff, noteSupply, onNoteChange = { note = it }, onHandoffChange = { noteHandoff = it }, onSupplyListChange = { noteSupply = it })
            SaveItemButton(formError) {
                val qty = positiveCount(count) ?: run { formError = "Enter at least 1 sandwich"; return@SaveItemButton }
                onSave(CateringItem.Sandwiches(trayType, portion, qty, condiments, note, noteHandoff, noteSupply))
            }
        }
    }
}

@Composable
private fun WrapsForm(existing: CateringItem.Wraps?, onSave: (CateringItem) -> Unit) {
    var count       by remember { mutableStateOf(existing?.count?.toString() ?: "") }
    var note        by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply  by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var formError   by remember { mutableStateOf<String?>(null) }
    SectionCard {
        SectionLabel("Wraps")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatNumberField("Total number of wraps", count) { count = it; formError = null }
            val n = count.toIntOrNull() ?: 0
            if (n > 0) Text("→ 15 per catering box (box count calculated per order total)", color = Blue, fontSize = 13.sp)
            NoteField(note, noteHandoff, noteSupply, onNoteChange = { note = it }, onHandoffChange = { noteHandoff = it }, onSupplyListChange = { noteSupply = it })
            SaveItemButton(formError) {
                val qty = positiveCount(count) ?: run { formError = "Enter at least 1 wrap"; return@SaveItemButton }
                onSave(CateringItem.Wraps(qty, note, noteHandoff, noteSupply))
            }
        }
    }
}

@Composable
private fun SaladForm(existing: CateringItem.Salad?, onSave: (CateringItem) -> Unit) {
    var saladType       by remember { mutableStateOf(existing?.saladType ?: SaladType.CARUSOS) }
    var selectedSize            by remember { mutableStateOf(existing?.saladSize ?: SaladSize.FEEDS_15) }
    var quantity        by remember { mutableStateOf(existing?.quantity?.toString() ?: "1") }
    var addChicken      by remember { mutableStateOf(existing?.addChicken ?: false) }
    var gardenDressings by remember { mutableStateOf(existing?.gardenDressings ?: emptySet<GardenDressing>()) }
    var croutonsOnSide  by remember { mutableStateOf(existing?.croutonsOnSide ?: false) }
    var customDressing  by remember { mutableStateOf(existing?.customDressing ?: false) }
    var note            by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff     by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply      by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var formError       by remember { mutableStateOf<String?>(null) }

    val showDressingPicker = saladType.usesCustomDressing || (saladType.hasDefaultDressing && customDressing)

    SectionCard {
        SectionLabel("Salad")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatChipRow("Type", SaladType.values().map { it.label }, SaladType.values().indexOf(saladType)) {
                val newType = SaladType.values()[it]
                saladType = newType
                if (!newType.hasDefaultDressing) customDressing = false
                if (!(newType.usesCustomDressing || (newType.hasDefaultDressing && customDressing))) gardenDressings = emptySet()
            }
            CatChipRow("Size", SaladSize.values().map { it.label }, SaladSize.values().indexOf(selectedSize)) { selectedSize = SaladSize.values()[it] }
            CatNumberField("Quantity", quantity) { quantity = it; formError = null }
            if (!saladType.isPasta && !saladType.usesCustomDressing) CatToggleRow("Add chicken", addChicken) { addChicken = it }

            if (saladType.hasDefaultDressing) {
                CatToggleRow("Custom dressings", customDressing) { checked ->
                    customDressing = checked
                    if (!checked) gardenDressings = emptySet()
                }
            }

            if (showDressingPicker) {
                // Feeds 15 → up to 2 full dressings. Feeds 7 → up to 2 half dressings (still 2 picks).
                val maxDressings = 2
                val dressingUnitLabel = if (selectedSize == SaladSize.FEEDS_7) "half dressings" else "dressings"
                Text("DRESSING (pick up to $maxDressings $dressingUnitLabel)", color = TextDim, fontSize = 11.sp, letterSpacing = 1.1.sp)
                GardenDressing.values().toList().chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { d ->
                            val sel = gardenDressings.contains(d)
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Blue else SurfaceDark)
                                .border(1.5.dp, if (sel) Blue else BorderLight, RoundedCornerShape(8.dp))
                                .clickable {
                                    gardenDressings = if (sel) gardenDressings - d
                                    else if (gardenDressings.size < maxDressings) gardenDressings + d
                                    else gardenDressings  // max reached
                                }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(d.label, color = if (sel) Background else TextMuted, fontSize = 12.sp)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (!saladType.isPasta) CatToggleRow("Croutons on side", croutonsOnSide) { croutonsOnSide = it }

            // Supply hint
            val qty = quantity.toIntOrNull() ?: 1
            val utensil = when { saladType.isPasta -> "spoon"; saladType.useSpoon -> "spoon"; else -> "large tong" }
            val dressingBottles = if (selectedSize == SaladSize.FEEDS_15) 2 else 1
            Text("→ $qty $utensil${if (qty != 1) "s" else ""}", color = TextDim, fontSize = 12.sp)
            when {
                saladType == SaladType.PIERCE && !showDressingPicker  -> Text("→ ${dressingBottles * qty} Poppyseed bottle${if (dressingBottles * qty != 1) "s" else ""}", color = TextDim, fontSize = 12.sp)
                saladType == SaladType.CAESAR && !showDressingPicker  -> Text("→ ${dressingBottles * qty} Caesar dressing bottle${if (dressingBottles * qty != 1) "s" else ""}", color = TextDim, fontSize = 12.sp)
                saladType == SaladType.CARUSOS && !showDressingPicker -> Text("→ ${if (selectedSize == SaladSize.FEEDS_15) "1 full" else "½"} Blue cheese + VnO × $qty", color = TextDim, fontSize = 12.sp)
                showDressingPicker -> {
                    val dList = gardenDressings.toList()
                    when {
                        dList.size == 2 && dressingBottles == 2 -> Text("→ 1 ${dList[0].label} + 1 ${dList[1].label} bottle per salad × $qty", color = TextDim, fontSize = 12.sp)
                        dList.size == 2 && dressingBottles == 1 -> Text("→ ½ ${dList[0].label} + ½ ${dList[1].label} bottle per salad × $qty", color = TextDim, fontSize = 12.sp)
                        dList.isNotEmpty() -> Text("→ $dressingBottles ${dList.joinToString(" + ") { it.label }} bottle${if (dressingBottles > 1) "s" else ""} × $qty", color = TextDim, fontSize = 12.sp)
                        else -> Text("→ $dressingBottles dressing bottle${if (dressingBottles > 1) "s" else ""} × $qty (no dressing selected)", color = TextDim, fontSize = 12.sp)
                    }
                }
            }
            if (croutonsOnSide) {
                val qty2 = quantity.toIntOrNull() ?: 1
                val feeds15 = if (selectedSize == SaladSize.FEEDS_15) qty2 else 0
                val feeds7  = if (selectedSize == SaladSize.FEEDS_7)  qty2 else 0
                val parts = buildList { if (feeds15 > 0) add("${feeds15}x feeds 15"); if (feeds7 > 0) add("${feeds7}x feeds 7") }
                val feeds15Equiv = if (selectedSize == SaladSize.FEEDS_15) qty2.toDouble() else qty2 / 2.0
                val croutonSpoons = Math.ceil(feeds15Equiv / 2.0).toInt().coerceAtLeast(1)
                Text("→ Side croutons for ${parts.joinToString(" and ")} + ${croutonSpoons} spoon${if (croutonSpoons != 1) "s" else ""}", color = TextDim, fontSize = 12.sp)
            }
            NoteField(note, noteHandoff, noteSupply, onNoteChange = { note = it }, onHandoffChange = { noteHandoff = it }, onSupplyListChange = { noteSupply = it })
            SaveItemButton(formError) {
                val qty = positiveCount(quantity) ?: run { formError = "Quantity must be at least 1"; return@SaveItemButton }
                if (showDressingPicker && gardenDressings.isEmpty()) {
                    formError = "Pick at least one dressing for ${saladType.label} salad"
                    return@SaveItemButton
                }
                onSave(CateringItem.Salad(saladType, selectedSize, qty, addChicken, gardenDressings, croutonsOnSide, customDressing, note, noteHandoff, noteSupply))
            }
        }
    }
}

@Composable
private fun PastaForm(existing: CateringItem.PastaTray?, onSave: (CateringItem) -> Unit) {
    var pastaType   by remember { mutableStateOf(existing?.pastaType ?: PastaType.BYO) }
    var selectedSize        by remember { mutableStateOf(existing?.pastaSize ?: PastaSize.FEEDS_15) }
    var quantity    by remember { mutableStateOf(existing?.quantity?.toString() ?: "1") }
    var byoNoodle   by remember { mutableStateOf(existing?.byoNoodle ?: BYONoodle.PENNE) }
    var byoSauce    by remember { mutableStateOf(existing?.byoSauce ?: BYOSauce.MARINARA) }
    var addOns      by remember { mutableStateOf(existing?.addOns ?: emptyList()) }
    var note        by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply  by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var formError   by remember { mutableStateOf<String?>(null) }
    SectionCard {
        SectionLabel("Pasta")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatChipRow("Type", PastaType.values().map { it.label }, PastaType.values().indexOf(pastaType)) { pastaType = PastaType.values()[it] }
            if (pastaType == PastaType.BYO) {
                CatChipRow("Noodle", BYONoodle.values().map { it.label }, BYONoodle.values().indexOf(byoNoodle)) { byoNoodle = BYONoodle.values()[it] }
                CatChipRow("Sauce", BYOSauce.values().map { it.label }, BYOSauce.values().indexOf(byoSauce)) { byoSauce = BYOSauce.values()[it] }
            }
            CatChipRow("Size", PastaSize.values().map { it.label }, PastaSize.values().indexOf(selectedSize)) { selectedSize = PastaSize.values()[it] }
            CatNumberField("Quantity", quantity) { quantity = it; formError = null }
            val usesTong = if (pastaType == PastaType.BYO) byoNoodle == BYONoodle.SPAGHETTI else pastaType.usesTong
            val qty = quantity.toIntOrNull() ?: 1
            Text("→ $qty ${if (usesTong) "large tong${if (qty != 1) "s" else ""}" else "spoon${if (qty != 1) "s" else ""}"}", color = TextDim, fontSize = 12.sp)
            Text("ADD-ONS", color = TextDim, fontSize = 11.sp, letterSpacing = 1.2.sp)
            listOf("Chicken", "Bacon", "Italian Sausage").forEach { addon ->
                val checked = addOns.contains(addon)
                Row(modifier = Modifier.fillMaxWidth().clickable { addOns = if (checked) addOns - addon else addOns + addon }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(addon, color = TextSecondary, fontSize = 14.sp); Toggle(checked) { addOns = if (checked) addOns - addon else addOns + addon }
                }
            }
            NoteField(note, noteHandoff, noteSupply, onNoteChange = { note = it }, onHandoffChange = { noteHandoff = it }, onSupplyListChange = { noteSupply = it })
            SaveItemButton(formError) {
                val qty = positiveCount(quantity) ?: run { formError = "Quantity must be at least 1"; return@SaveItemButton }
                onSave(CateringItem.PastaTray(pastaType, selectedSize, qty, if (pastaType == PastaType.BYO) byoNoodle else null, if (pastaType == PastaType.BYO) byoSauce else null, addOns, note, noteHandoff, noteSupply))
            }
        }
    }
}

@Composable
private fun BoxLunchForm(existing: CateringItem.BoxLunch?, onSave: (CateringItem) -> Unit) {
    var sandwichType by remember { mutableStateOf(existing?.sandwichType ?: SandwichTrayType.CLASSIC) }
    var isWrap       by remember { mutableStateOf(existing?.isWrap ?: false) }
    var count        by remember { mutableStateOf(existing?.count?.toString() ?: "") }
    var drink        by remember { mutableStateOf(existing?.drinkChoice ?: DrinkChoice.PEPSI) }
    var pepsiCount   by remember { mutableStateOf(existing?.pepsiCount?.toString() ?: "") }
    var waterCount   by remember { mutableStateOf(existing?.waterCount?.toString() ?: "") }
    var note         by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff  by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply   by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var formError    by remember { mutableStateOf<String?>(null) }
    SectionCard {
        SectionLabel("Box Lunches")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to "Sandwich", true to "Wrap").forEach { (wrap, label) ->
                    val sel = isWrap == wrap
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (sel) Blue else SurfaceDark).border(1.5.dp, if (sel) Blue else BorderLight, RoundedCornerShape(8.dp)).clickable { isWrap = wrap }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text(label, color = if (sel) Background else TextMuted, fontSize = 14.sp) }
                }
            }
            if (!isWrap) CatChipRow("Sandwich type", SandwichTrayType.values().map { it.label }, SandwichTrayType.values().indexOf(sandwichType)) { sandwichType = SandwichTrayType.values()[it] }
            CatNumberField("Number of box lunches", count) { count = it; formError = null }
            val n = count.toIntOrNull() ?: 0
            if (n > 0) Text("→ ${n / 2 + n % 2} choc + ${n / 2} mac cookies · $n chips", color = TextDim, fontSize = 12.sp)
            Text("DRINKS", color = TextDim, fontSize = 11.sp, letterSpacing = 1.2.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DrinkChoice.values().forEach { choice ->
                    val sel = drink == choice
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (sel) Blue else SurfaceDark).border(1.5.dp, if (sel) Blue else BorderLight, RoundedCornerShape(8.dp)).clickable { drink = choice }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(choice.name, color = if (sel) Background else TextMuted, fontSize = 12.sp) }
                }
            }
            if (drink == DrinkChoice.SPLIT) {
                CatNumberField("Pepsi cans", pepsiCount) { pepsiCount = it; formError = null }
                CatNumberField("Water bottles", waterCount) { waterCount = it; formError = null }
            }
            NoteField(note, noteHandoff, noteSupply, "e.g. specific Pepsi flavors", onNoteChange = { note = it }, onHandoffChange = { noteHandoff = it }, onSupplyListChange = { noteSupply = it })
            SaveItemButton(formError) {
                val n2 = positiveCount(count) ?: run { formError = "Enter at least 1 box lunch"; return@SaveItemButton }
                val pc = if (drink == DrinkChoice.PEPSI) n2 else pepsiCount.toIntOrNull() ?: 0
                val wc = if (drink == DrinkChoice.WATER) n2 else waterCount.toIntOrNull() ?: 0
                if (drink == DrinkChoice.SPLIT && pc + wc != n2) {
                    formError = "Pepsi + water must equal $n2 box lunches"
                    return@SaveItemButton
                }
                onSave(CateringItem.BoxLunch(n2, sandwichType, isWrap, drink, pc, wc, note, noteHandoff, noteSupply))
            }
        }
    }
}

@Composable
private fun PizzaForm(existing: CateringItem.Pizza?, onSave: (CateringItem) -> Unit) {
    var count by remember { mutableStateOf(existing?.count?.toString() ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var formError by remember { mutableStateOf<String?>(null) }
    SectionCard { SectionLabel("Pizza"); Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { CatNumberField("Number of pizzas", count) { count = it; formError = null }; NoteField(note, noteHandoff, noteSupply, "e.g. specific pizza types", onNoteChange = { note = it }, onHandoffChange = { noteHandoff = it }, onSupplyListChange = { noteSupply = it }); SaveItemButton(formError) { val qty = positiveCount(count) ?: run { formError = "Enter at least 1 pizza"; return@SaveItemButton }; onSave(CateringItem.Pizza(qty, note, noteHandoff, noteSupply)) } } }
}

@Composable
private fun DessertForm(existing: CateringItem.Dessert?, onSave: (CateringItem) -> Unit) {
    var dessertType by remember { mutableStateOf(existing?.dessertType ?: DessertType.COOKIE_PLATTER) }
    var count by remember { mutableStateOf(existing?.count?.toString() ?: "1") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var formError by remember { mutableStateOf<String?>(null) }
    SectionCard {
        SectionLabel("Dessert")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatChipRow("Type", DessertType.values().map { it.label }, DessertType.values().indexOf(dessertType)) { dessertType = DessertType.values()[it] }
            CatNumberField("Quantity", count) { count = it; formError = null }
            Text("→ ${when (dessertType) { DessertType.COOKIE_PLATTER -> "24 cookies (12 choc + 12 mac) + 1 small tong"; DessertType.BROWNIE_COOKIE -> "3 brownies + 14 cookies (7+7) + 1 small tong"; DessertType.BROWNIE_PLATTER -> "10 brownies + 1 small tong" }}", color = TextDim, fontSize = 12.sp)
            NoteField(note, noteHandoff, noteSupply, onNoteChange = { note = it }, onHandoffChange = { noteHandoff = it }, onSupplyListChange = { noteSupply = it })
            SaveItemButton(formError) {
                val qty = positiveCount(count) ?: run { formError = "Quantity must be at least 1"; return@SaveItemButton }
                onSave(CateringItem.Dessert(dessertType, qty, note, noteHandoff, noteSupply))
            }
        }
    }
}

@Composable
private fun IndividualItemsForm(existing: CateringItem.IndividualItems?, onSave: (CateringItem) -> Unit) {
    var itemType    by remember { mutableStateOf(existing?.itemType ?: IndividualItemType.LARGE_SALAD) }
    var count       by remember { mutableStateOf(existing?.count?.toString() ?: "") }
    var note        by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply  by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var formError   by remember { mutableStateOf<String?>(null) }

    SectionCard {
        SectionLabel("Individual Items")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatChipRow(
                "Type",
                IndividualItemType.values().map { it.label },
                IndividualItemType.values().indexOf(itemType),
            ) { itemType = IndividualItemType.values()[it]; formError = null }

            CatNumberField("Quantity", count) { count = it; formError = null }

            // Live hint
            val n = count.toIntOrNull() ?: 0
            if (n > 0) {
                val hint = when (itemType) {
                    IndividualItemType.LARGE_SALAD   -> "→ ${n * 2} scoops of lettuce"
                    IndividualItemType.SMALL_SALAD   -> "→ $n scoop${if (n != 1) "s" else ""} of lettuce"
                    IndividualItemType.HALF_SANDWICH -> "→ ${n * 2}/4 of a loaf (½ sandwich each)"
                    IndividualItemType.FULL_SANDWICH -> "→ $n full loaf${if (n != 1) "s" else ""}"
                }
                Text(hint, color = TextDim, fontSize = 12.sp)
            }

            NoteField(
                note, noteHandoff, noteSupply,
                onNoteChange       = { note = it },
                onHandoffChange    = { noteHandoff = it },
                onSupplyListChange = { noteSupply = it },
            )
            SaveItemButton(formError) {
                val qty = positiveCount(count) ?: run { formError = "Enter at least 1"; return@SaveItemButton }
                onSave(CateringItem.IndividualItems(itemType, qty, note, noteHandoff, noteSupply))
            }
        }
    }
}

@Composable
private fun ExtrasForm(existing: CateringItem.Extra?, onSave: (CateringItem) -> Unit) {
    var extraType      by remember { mutableStateOf(existing?.extraType ?: ExtraType.WATER_BOTTLES) }
    var count          by remember { mutableStateOf(existing?.count?.toString() ?: "") }
    var cookieFlavor   by remember { mutableStateOf(existing?.cookieFlavor) }
    var dressingChoice by remember { mutableStateOf(existing?.dressingChoice) }
    var otherNote      by remember { mutableStateOf(existing?.otherNote ?: "") }
    var note           by remember { mutableStateOf(existing?.note ?: "") }
    var noteHandoff    by remember { mutableStateOf(existing?.noteInHandoff ?: true) }
    var noteSupply     by remember { mutableStateOf(existing?.noteInSupplyList ?: false) }
    var formError      by remember { mutableStateOf<String?>(null) }

    SectionCard {
        SectionLabel("Extras")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CatChipRow("Type", ExtraType.values().map { it.label }, ExtraType.values().indexOf(extraType)) { extraType = ExtraType.values()[it]; cookieFlavor = null; dressingChoice = null }
            CatNumberField(if (extraType == ExtraType.SOUP) "Number of cups" else "Quantity", count) { count = it; formError = null }
            if (extraType == ExtraType.SOUP) {
                val n = count.toIntOrNull() ?: 0
                if (n > 0) Text("→ $n breadstick${if (n != 1) "s" else ""} included", color = TextDim, fontSize = 12.sp)
            }

            // Cookie flavor sub-picker
            if (extraType == ExtraType.COOKIES) {
                Text("FLAVOR", color = TextDim, fontSize = 11.sp, letterSpacing = 1.1.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CookieFlavor.values().forEach { flavor ->
                        val sel = cookieFlavor == flavor
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (sel) Blue else SurfaceDark).border(1.5.dp, if (sel) Blue else BorderLight, RoundedCornerShape(8.dp)).clickable { cookieFlavor = if (sel) null else flavor }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(flavor.label, color = if (sel) Background else TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                if (cookieFlavor == null) Text("No flavor selected — defaults to 50/50 split", color = TextDim, fontSize = 12.sp)
            }

            // Dressing bottle sub-picker
            if (extraType == ExtraType.DRESSING_BOTTLE) {
                Text("DRESSING TYPE", color = TextDim, fontSize = 11.sp, letterSpacing = 1.1.sp)
                GardenDressing.values().toList().chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { d ->
                            val sel = dressingChoice == d
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (sel) Blue else SurfaceDark).border(1.5.dp, if (sel) Blue else BorderLight, RoundedCornerShape(8.dp)).clickable { dressingChoice = if (sel) null else d }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(d.label, color = if (sel) Background else TextMuted, fontSize = 12.sp)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (extraType == ExtraType.OTHER) CatTextField("Specify what", otherNote) { otherNote = it }
            NoteField(note, noteHandoff, noteSupply, onNoteChange = { note = it }, onHandoffChange = { noteHandoff = it }, onSupplyListChange = { noteSupply = it })
            SaveItemButton(formError) {
                val n = positiveCount(count) ?: run { formError = "Enter a quantity of at least 1"; return@SaveItemButton }
                if (extraType == ExtraType.DRESSING_BOTTLE && dressingChoice == null) {
                    formError = "Pick a dressing type"
                    return@SaveItemButton
                }
                val flavor = if (extraType == ExtraType.COOKIES) (cookieFlavor ?: CookieFlavor.FIFTY_FIFTY) else null
                onSave(CateringItem.Extra(extraType, n, flavor, dressingChoice, otherNote, note, noteHandoff, noteSupply))
            }
        }
    }
}

// ─── Shared form components ───────────────────────────────────────────────────

@Composable
private fun TimeInputField(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("TIME", color = TextDim, fontSize = 11.sp, letterSpacing = 1.1.sp)
        val presets = listOf("10:00 AM", "10:30 AM", "11:00 AM", "11:15 AM", "11:30 AM", "12:00 PM", "12:30 PM", "1:00 PM")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            presets.chunked(4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { preset ->
                        val sel = value == preset
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(if (sel) Blue else SurfaceDark).border(1.dp, if (sel) Blue else BorderLight, RoundedCornerShape(6.dp)).clickable { onChange(preset) }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) { Text(preset, color = if (sel) Background else TextDim, fontSize = 11.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        BasicTextField(value = value, onValueChange = onChange, singleLine = true, textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp), cursorBrush = SolidColor(Blue), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp), decorationBox = { inner -> if (value.isEmpty()) Text("Or type a custom time…", color = TextDim, fontSize = 15.sp); inner() })
    }
}

@Composable
private fun NoteField(
    note: String,
    noteInHandoff: Boolean,
    noteInSupplyList: Boolean,
    placeholder: String = "Note (optional)",
    onNoteChange: (String) -> Unit,
    onHandoffChange: (Boolean) -> Unit,
    onSupplyListChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CatTextField(placeholder, note, minLines = 2) { onNoteChange(it) }
        if (note.isNotBlank()) {
            Text("Show this note on:", color = TextDim, fontSize = 11.sp, letterSpacing = 1.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Handoff message", color = TextSecondary, fontSize = 13.sp); Toggle(noteInHandoff) { onHandoffChange(!noteInHandoff) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Supply list", color = TextSecondary, fontSize = 13.sp); Toggle(noteInSupplyList) { onSupplyListChange(!noteInSupplyList) }
            }
        }
    }
}

@Composable
private fun CatTextField(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), color = TextDim, fontSize = 11.sp, letterSpacing = 1.1.sp)
        BasicTextField(value = value, onValueChange = onChange, textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp), cursorBrush = SolidColor(Blue), minLines = minLines, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp), decorationBox = { inner -> if (value.isEmpty()) Text(label, color = TextDim, fontSize = 15.sp); inner() })
    }
}

@Composable
private fun CatNumberField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), color = TextDim, fontSize = 11.sp, letterSpacing = 1.1.sp)
        BasicTextField(value = value, onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(5)) }, singleLine = true, textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontFamily = FontFamily.Monospace), cursorBrush = SolidColor(Blue), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceDark).border(1.5.dp, BorderLight, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp), decorationBox = { inner -> if (value.isEmpty()) Text("0", color = TextDim, fontSize = 15.sp, fontFamily = FontFamily.Monospace); inner() })
    }
}

@Composable
private fun CatChipRow(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = TextDim, fontSize = 11.sp, letterSpacing = 1.1.sp)
        options.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { option ->
                    val idx = options.indexOf(option); val sel = idx == selected
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (sel) Blue else SurfaceDark).border(1.5.dp, if (sel) Blue else BorderLight, RoundedCornerShape(8.dp)).clickable { onSelect(idx) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) { Text(option, color = if (sel) Background else TextMuted, fontSize = 12.sp) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun CatToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, color = TextSecondary, fontSize = 14.sp); Toggle(value) { onChange(!value) } }
}

private fun positiveCount(value: String): Int? = value.toIntOrNull()?.takeIf { it > 0 }

@Composable private fun SaveItemButton(error: String? = null, onSave: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (error != null) Text(error, color = ErrorText, fontSize = 13.sp)
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Blue).clickable(onClick = onSave).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text("Save & Add Another", color = Background, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CateringCrewActions(
    copiedHandoff: Boolean,
    onCopyHandoff: () -> Unit,
    onEmail: () -> Unit,
    onPrint: () -> Unit,
) {
    var showPdfActions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.5.dp, BorderLight, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopyHandoff)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📤", fontSize = 16.sp)
            Text(
                if (copiedHandoff) "Handoff copied!" else "Copy crew handoff",
                color = if (copiedHandoff) Green else Blue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPdfActions = !showPdfActions }
                .padding(start = 14.dp, end = 14.dp, bottom = if (showPdfActions) 4.dp else 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${if (showPdfActions) "Hide" else "Print & email"} PDF ${if (showPdfActions) "▲" else "▼"}",
                color = TextDim,
                fontSize = 11.sp,
            )
        }

        if (showPdfActions) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExportActionChip("📧", "Email", Green, Modifier.weight(1f), onEmail)
                ExportActionChip("🖨️", "Print", Blue, Modifier.weight(1f), onPrint)
            }
        }
    }
}

@Composable
private fun ExportActionChip(
    emoji: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 14.sp)
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable private fun SmallBtn(label: String, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) { Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun LocationChoiceButton(
    name: String,
    email: String,
    color: Color,
    onPick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onPick(email) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(email, color = TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}