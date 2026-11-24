package com.tippool.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tippool.AppSettings
import com.tippool.ui.theme.*

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
) {
    var closingEnabled by remember { mutableStateOf(settings.closingReminderEnabled) }
    var closingDelay   by remember { mutableStateOf(settings.closingReminderDelayMinutes.toString()) }
    var closingItems   by remember { mutableStateOf(settings.closingReminderItems) }

    var morningEnabled by remember { mutableStateOf(settings.morningReminderEnabled) }
    var morningDelay   by remember { mutableStateOf(settings.morningReminderDelayMinutes.toString()) }
    var morningItems   by remember { mutableStateOf(settings.morningReminderItems) }
    var saturdayItems  by remember { mutableStateOf(settings.saturdayExtraItems) }

    var locationEmail   by remember { mutableStateOf(settings.locationEmail ?: "") }
    var useCustomEmail  by remember { mutableStateOf(settings.useCustomEmail) }
    var customEmail     by remember { mutableStateOf(settings.customEmail) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding()
            .verticalScroll(rememberScrollState())
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
                    Text("Settings", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Text("REMINDERS & NOTIFICATIONS", color = TextDim, fontSize = 11.sp, letterSpacing = 2.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Orange)
                    .clickable {
                        onSave(
                            AppSettings(
                                closingReminderEnabled      = closingEnabled,
                                closingReminderDelayMinutes = closingDelay.toIntOrNull()?.coerceAtLeast(0) ?: 15,
                                closingReminderItems        = closingItems.filter { it.isNotBlank() },
                                morningReminderEnabled      = morningEnabled,
                                morningReminderDelayMinutes = morningDelay.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                                morningReminderItems        = morningItems.filter { it.isNotBlank() },
                                saturdayExtraItems          = saturdayItems.filter { it.isNotBlank() },
                                locationEmail  = locationEmail.ifBlank { null },
                                useCustomEmail = useCustomEmail,
                                customEmail    = customEmail,
                                )
                        )
                        onBack()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Save", color = Background, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Closing reminder section
        SettingsSection(
            title    = "Closing Reminder",
            subtitle = "Fires after calculation when it's 5 PM or later",
        ) {
            ToggleRow("Enable closing reminder", closingEnabled) { closingEnabled = !closingEnabled }
            if (closingEnabled) {
                Spacer(Modifier.height(2.dp))
                DelayRow("Minutes after calculation", closingDelay) { closingDelay = it }
                Spacer(Modifier.height(4.dp))
                ReminderItemList("Reminder items", closingItems, Orange) { closingItems = it }
            }
        }

        // Morning reminder section
        SettingsSection(
            title    = "Morning Reminder",
            subtitle = "Fires after calculation before 5 PM",
        ) {
            ToggleRow("Enable morning reminder", morningEnabled) { morningEnabled = !morningEnabled }
            if (morningEnabled) {
                Spacer(Modifier.height(2.dp))
                DelayRow("Minutes after calculation", morningDelay, hint = "0 = immediate") { morningDelay = it }
                Spacer(Modifier.height(4.dp))
                ReminderItemList("Reminder items", morningItems, Orange) { morningItems = it }
                Spacer(Modifier.height(4.dp))
                ReminderItemList("Saturday-only extras", saturdayItems, Blue) { saturdayItems = it }
            }
        }

        SettingsSection(
            title    = "Catering Email",
            subtitle = "Where the print packet gets sent",
        ) {
            // Location picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LOCATION", color = TextDim, fontSize = 11.sp, letterSpacing = 1.2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Argonne" to "location-a@example.com",
                        "CDA"     to "location-b@example.com",
                    ).forEach { (name, email) ->
                        val selected = locationEmail == email && !useCustomEmail
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Orange.copy(alpha = 0.15f) else SurfaceDark)
                                .border(
                                    1.5.dp,
                                    if (selected) Orange else BorderLight,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable {
                                    locationEmail  = email
                                    useCustomEmail = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(name, color = if (selected) Orange else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(email, color = TextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // Custom email toggle
            ToggleRow("Use custom email", useCustomEmail) { useCustomEmail = !useCustomEmail }

            if (useCustomEmail) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CUSTOM EMAIL", color = TextDim, fontSize = 11.sp, letterSpacing = 1.2.sp)
                    BasicTextField(
                        value         = customEmail,
                        onValueChange = { customEmail = it },
                        singleLine    = true,
                        textStyle     = TextStyle(color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                        cursorBrush   = SolidColor(Orange),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier      = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(1.5.dp, Orange.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        decorationBox = { inner ->
                            if (customEmail.isEmpty()) Text("you@example.com", color = TextDim, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            inner()
                        },
                    )
                }
            }

            // Preview of what will actually be used
            val preview = when {
                useCustomEmail && customEmail.isNotBlank() -> customEmail
                locationEmail.isNotBlank()                 -> locationEmail
                else                                       -> "Not set"
            }
            Text("Sending to: $preview", color = TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionCard {
        Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Toggle(checked = checked, onToggle = onToggle)
    }
}

@Composable
private fun DelayRow(
    label: String,
    value: String,
    hint: String = "",
    onChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, color = TextSecondary, fontSize = 14.sp)
            if (hint.isNotEmpty()) Text(hint, color = TextDim, fontSize = 11.sp)
        }
        Row(
            modifier = Modifier
                .width(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.5.dp, BorderLight, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value           = value,
                onValueChange   = { if (it.length <= 3) onChange(it) },
                singleLine      = true,
                textStyle       = TextStyle(color = TextPrimary, fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                cursorBrush     = SolidColor(Orange),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier        = Modifier.weight(1f),
                decorationBox   = { inner ->
                    if (value.isEmpty()) Text("0", color = TextDim, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                    inner()
                },
            )
            Text(" min", color = TextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ReminderItemList(
    label: String,
    items: List<String>,
    accentColor: Color,
    onChange: (List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = TextDim, fontSize = 11.sp, letterSpacing = 1.2.sp)

        items.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accentColor))
                BasicTextField(
                    value         = item,
                    onValueChange = { new -> onChange(items.toMutableList().also { it[index] = new }) },
                    singleLine    = true,
                    textStyle     = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    cursorBrush   = SolidColor(accentColor),
                    modifier      = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (item.isEmpty()) Text("Reminder item…", color = TextDim, fontSize = 14.sp)
                        inner()
                    },
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(BorderLight)
                        .clickable { onChange(items.toMutableList().also { it.removeAt(index) }) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = TextMuted, fontSize = 10.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onChange(items + "") }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("+", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Add item", color = accentColor, fontSize = 13.sp)
        }
    }
}