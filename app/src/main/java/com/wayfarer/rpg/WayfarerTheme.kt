package com.wayfarer.rpg

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF07110E)
val Surface = Color(0xFF0D1C17)
val Card = Color(0xFF10251B)
val CardAlt = Color(0xFF132D21)
val Gold = Color(0xFFD8B97B)
val GoldDark = Color(0xFF8D6A3E)
val Green = Color(0xFF72DE86)
val GreenDark = Color(0xFF1D5A35)
val Text = Color(0xFFF3E8CF)
val Muted = Color(0xFFA9B4AA)
val Danger = Color(0xFFC84B50)

private val Colors = darkColorScheme(
    primary = Green,
    secondary = Gold,
    background = Bg,
    surface = Surface,
    onPrimary = Bg,
    onSecondary = Bg,
    onBackground = Text,
    onSurface = Text
)

@Composable
fun WayfarerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 31.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 14.sp)
        ),
        content = content
    )
}
