package com.attendance.pro.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Black = Color(0xFF0A0A0C)
val Charcoal = Color(0xFF141418)
val Graphite = Color(0xFF1E1F25)
val Slate = Color(0xFF2A2C34)
val Silver = Color(0xFFC9CED6)
val SilverBright = Color(0xFFE8EBF0)
val Accent = Color(0xFF8FA3C0)
val LateRed = Color(0xFFFF4D4F)
val OkGreen = Color(0xFF3DDC97)
val Amber = Color(0xFFFFB84D)

val Neon = Color(0xFF6C8CFF)
val Violet = Color(0xFF9B6CFF)
val Cyan = Color(0xFF35E0FF)

val BackgroundBrush = Brush.verticalGradient(listOf(Black, Charcoal, Black))

private val Scheme = darkColorScheme(
    primary = Silver, onPrimary = Black,
    secondary = Accent, onSecondary = Black,
    background = Black, onBackground = SilverBright,
    surface = Graphite, onSurface = SilverBright,
    surfaceVariant = Slate, onSurfaceVariant = Silver,
    error = LateRed
)

@Composable
fun AttendanceTheme(content: @Composable () -> Unit) {
    // Intentionally dark-only (premium black/silver look).
    MaterialTheme(colorScheme = Scheme, content = content)
}

/** Deep dark background with soft violet / cyan "aurora" glows. */
fun Modifier.auroraBackground(): Modifier = this
    .background(BackgroundBrush)
    .drawBehind {
        drawRect(
            Brush.radialGradient(
                listOf(Violet.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.04f), radius = size.width * 0.95f
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(Cyan.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.98f, size.height * 0.60f), radius = size.width * 0.85f
            )
        )
    }
