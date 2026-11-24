package com.tippool.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val TipPoolColorScheme = darkColorScheme(
    background     = Background,
    surface        = Surface,
    primary        = Orange,
    onPrimary      = Background,
    onBackground   = TextPrimary,
    onSurface      = TextPrimary,
)

@Composable
fun TipPoolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TipPoolColorScheme,
        content     = content
    )
}