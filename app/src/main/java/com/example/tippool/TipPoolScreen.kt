package com.tippool.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.tippool.AppSettings
import com.tippool.CalculationResult
import com.tippool.TIERS
import com.tippool.TierResult
import com.tippool.TipPoolViewModel
import com.tippool.loadSettings
import com.tippool.save
import com.tippool.scheduleSmartReminder
import com.tippool.ui.theme.*
import androidx.core.net.toUri

val TIER_COLORS = mapOf<Int, Color>(
    100 to Orange,
    75  to Blue,
    50  to Green,
    25  to Purple,
)

val OPENING_CHECKLIST = listOf(
    "Check blast chiller",
    "Open signs off",
    "Check next day orders",
    "Complete delivery paid outs before counting tips",
)

@Composable
fun TipPoolScreen(vm: TipPoolViewModel) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(loadSettings(context)) }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            onBack   = { showSettings = false },
            onSave   = { newSettings ->
                save(context, newSettings)
                settings = newSettings
            },
        )
        return
    }

    val scrollState    = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var calculateButtonOffset by remember { mutableIntStateOf(0) }
    var calculateButtonHeight by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(8.dp).height(32.dp).clip(RoundedCornerShape(4.dp)).background(Orange))
                Column {
                    Text("Tip Pool", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Text("SHIFT CALCULATOR", color = TextDim, fontSize = 11.sp, letterSpacing = 2.sp)
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .border(1.5.dp, BorderLight, RoundedCornerShape(10.dp))
                    .clickable { showSettings = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙", color = TextSecondary, fontSize = 18.sp)
            }
        }

// Version + Bug Report Section
        var showChangelog by remember { mutableStateOf(false) }

        if (showChangelog) {
            AlertDialog(
                onDismissRequest = { showChangelog = false },
                containerColor = Surface,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Changelog", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("What's new in TipPool", color = TextDim, fontSize = 12.sp, letterSpacing = 0.5.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        ChangelogEntry(
                            version = "1.2.0",
                            date = "September 2026",
                            items = listOf(
                                "Added Saved Days — archive orders under any date and reopen them later; edits to an open saved day autosave just like today",
                                "Added Export/Import so shift leads can share a day's orders with each other as text, then import straight into Current Catering or a saved date",
                                "Added a Delivery Notes list under Heads Up, with a warning for deliveries within 15 minutes of each other",
                                "Added Cobb and Chef salads",
                                "Pierce, Caesar, and Caruso's House salads can now switch to custom dressings with a toggle",
                                "Garden-style salads at Feeds 7 can now split 2 half dressings instead of just 1 full dressing",
                                "Orders now autosave continually while you're editing, so closing the app early won't lose progress",
                                "Handoff message now splits into Before 2 PM / After 2 PM sections when needed",
                                "Reorganized handoff and report sections; Soup moved into Extras instead of its own type",
                                "Plates and Napkins are now separate toggles instead of one combined toggle",
                                "Forks and Serving Utensils are now separate toggles instead of one combined toggle",
                                "Extra dressing bottles now combine with salad dressing counts instead of listing separately",
                                "Removed the word \"Extra\" from Extras item names",
                            )
                        )
                        ChangelogEntry(
                            version = "1.1.0",
                            date = "April 2026",
                            items = listOf(
                                "Pastas now show a breadstick count (rounded up to nearest 3)",
                                "Orders with Pastas now get 1 small tong for breadsticks",
                                "Salads now have a side croutons toggle (1 small spoon per 2 feeds-15 salads)",
                                "Item save confirmation is now more visible",
                                "PDF layout updated — each order prints on its own page",
                                "PDF section headers now use dashed lines to save ink",
                                "Handoff message now includes the date of catering",
                                "Added the ability to choose location and add a custom email from settings (for PDF)"
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChangelog = false }) {
                        Text("Close", color = Orange, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Version 1.2.0",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderLight, RoundedCornerShape(6.dp))
                        .clickable { showChangelog = true }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "CHANGELOG",
                        color = Orange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                }
            }

            // Bug report box
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                    .clickable {
                        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = "sms:0000000000".toUri()
                            putExtra("sms_body", "Hey, I found a bug in TipPool...")
                        }
                        context.startActivity(smsIntent)
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Report bugs or request features: 000‑000‑0000",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }


        OpeningChecklistCard()

        InputSection(
            vm = vm,
            onButtonPositioned = { offset, height ->
                calculateButtonOffset = offset
                calculateButtonHeight = height
            },
            onCalculate = {
                vm.calculate()
                if (vm.result != null) {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(calculateButtonOffset + calculateButtonHeight + 32)
                    }
                    scheduleSmartReminder(context, settings)
                }
            },
        )

        val result = vm.result
        if (result != null) {
            ResultSection(result)
        }
    }
}

@Composable
private fun OpeningChecklistCard() {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.5.dp, Orange.copy(alpha = if (expanded) 0.4f else 0.2f), RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            // Collapsed bar — always visible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Orange))
                    Text("CHECKLIST", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp)
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = Orange.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                )
            }
            // Expanded items
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OPENING_CHECKLIST.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(Orange.copy(alpha = 0.5f)))
                            Text(item, color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun ChangelogEntry(version: String, date: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "v$version",
                color = Orange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Box(modifier = Modifier.size(3.dp).clip(RoundedCornerShape(2.dp)).background(TextDim))
            Text(date, color = TextDim, fontSize = 12.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("·", color = Orange.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(top = 1.dp))
                    Text(item, color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}
@Composable
private fun InputSection(
    vm: TipPoolViewModel,
    onButtonPositioned: (offset: Int, height: Int) -> Unit,
    onCalculate: () -> Unit,
) {
    SectionCard {
        SectionLabel("Employees by tier")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TIERS.forEach { tier ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TierDot(TIER_COLORS[tier.pct]!!)
                        Text(tier.label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    NumStepper(vm.counts[tier.pct] ?: 0) { vm.setCount(tier.pct, it) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total employees", color = TextDim, fontSize = 12.sp)
            Text(vm.totalEmployees.toString(), color = Orange, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }

    SectionCard {
        SectionLabel("Tips collected")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CurrencyInput("Card Tips", vm.cardTips) { vm.cardTips = it }
            CurrencyInput("Cash Tips", vm.cashTips) { vm.cashTips = it }
            val total = vm.liveTotal
            if (total > 0) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total pool", color = TextDim, fontSize = 12.sp)
                    Text("$${String.format("%.2f", total)}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }

    val error = vm.error
    if (error != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ErrorBg)
                .border(1.5.dp, ErrorBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("⚠ $error", color = ErrorText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
    }

    Button(
        onClick  = onCalculate,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .onGloballyPositioned { coords ->
                onButtonPositioned(coords.positionInParent().y.toInt(), coords.size.height)
            },
        shape  = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Background),
    ) {
        Text("Calculate", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun ResultSection(result: CalculationResult) {
    SectionCard {
        SectionLabel("Result")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            result.tierResults.forEach { tier -> TierResultCard(tier) }
            val hasExtras = result.extraDollars > 0
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hasExtras) ExtrasBg else SurfaceDark)
                    .border(1.5.dp, if (hasExtras) ExtrasBorder else Border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Extra dollars", color = TextSecondary, fontSize = 14.sp)
                        Text("$${result.extraDollars}", color = if (hasExtras) Green else TextDim, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    if (hasExtras) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${result.extraDollars} extra dollar${if (result.extraDollars != 1) "s" else ""} to give out",
                            color = ExtrasText, fontSize = 12.sp,
                        )
                    }
                }
            }
            // Cents below the whole-dollar pool go in the jar and are counted with the next pool.
            // Display only; hidden when there are none.
            if (result.leftoverCents > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Leave for next pool", color = TextSecondary, fontSize = 14.sp)
                    Text("$${String.format("%.2f", result.leftoverCents / 100.0)}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }

    SectionCard {
        SectionLabel("Math breakdown")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MathRow("Card tips",     "$${String.format("%.2f", result.cardTips)}")
            MathRow("Cash tips",     "$${String.format("%.2f", result.cashTips)}")
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            MathRow("Pool (whole $)", "$${result.pool}",                               valueColor = Orange)
            MathRow("Total shares",  String.format("%.2f", result.totalShares))
            MathRow("Value / share", "$${String.format("%.4f", result.valuePerShare)}")
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
            MathRow("Paid out",      "$${result.totalPaidOut}")
            MathRow("Remainder",     "$${result.extraDollars}",                        valueColor = Green)
        }
    }
}

@Composable
private fun TierResultCard(tier: TierResult) {
    val color = TIER_COLORS[tier.pct] ?: TextSecondary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.5.dp, color.copy(alpha = 0.13f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TierDot(color)
                    Text("${tier.count} × ${tier.label} employee${if (tier.count != 1) "s" else ""}", color = TextSecondary, fontSize = 14.sp)
                }
                Text("$${tier.flooredPayout}", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(4.dp))
            Text("each · $${tier.subtotal} total for this tier", color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(start = 18.dp))
        }
    }
}