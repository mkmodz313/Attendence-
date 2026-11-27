package com.attendance.pro.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.attendance.pro.ui.theme.*

private val RaisedShape = RoundedCornerShape(24.dp)

/**
 * Premium raised 3D box: coloured glow shadow, glossy top highlight, light rim,
 * a darker "thickness" edge underneath and a soft fade/slide-in when it appears.
 */
@Composable
fun Raised3DCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(if (shown) 1f else 0f, tween(450), label = "appear")

    val glow = accent ?: Color.Black
    val edge = accent?.copy(alpha = 0.50f) ?: Color(0xFF030304)
    val tint = accent?.copy(alpha = 0.16f) ?: Color.Transparent
    val fill = Brush.linearGradient(listOf(Color(0xFF3B3F4C), Color(0xFF242630), Color(0xFF16171C)))
    val rim = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent, Silver.copy(alpha = 0.10f)))

    Column(
        modifier = modifier
            .graphicsLayer { alpha = appear; translationY = (1f - appear) * 48f }
            .padding(bottom = 7.dp)
            .drawBehind {
                drawRoundRect(edge, Offset(0f, 7.dp.toPx()), size, CornerRadius(24.dp.toPx()))
            }
            .shadow(16.dp, RaisedShape, ambientColor = glow, spotColor = glow)
            .background(fill, RaisedShape)
            .background(Brush.horizontalGradient(listOf(tint, Color.Transparent)), RaisedShape)
            .drawBehind {
                // glossy highlight across the upper half
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), endY = size.height * 0.55f),
                    size = Size(size.width, size.height * 0.55f), cornerRadius = CornerRadius(24.dp.toPx())
                )
            }
            .border(1.dp, rim, RaisedShape)
            .padding(16.dp),
        content = content
    )
}

/** 3D toggle button with icon; springs down when pressed and glows when selected. */
@Composable
fun StatusPill(
    label: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.90f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "press"
    )
    val shape = RoundedCornerShape(16.dp)
    val bg = if (selected) Brush.verticalGradient(listOf(color, color.copy(alpha = 0.65f)))
    else Brush.verticalGradient(listOf(Color(0xFF343642), Color(0xFF1C1D24)))
    val fg = if (selected) Color(0xFF08090B) else Silver

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (selected) 12.dp else 4.dp, shape, ambientColor = if (selected) color else Color.Black, spotColor = if (selected) color else Color.Black)
            .background(bg, shape)
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.45f) else Slate, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = fg, modifier = Modifier.size(20.dp))
        Text(label, maxLines = 1, color = fg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
    }
}

/** Animated circular progress ring with a centred label. */
@Composable
fun ProgressRing(progress: Float, label: String, modifier: Modifier = Modifier, stroke: Dp = 9.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(stroke / 2)) {
            val st = Stroke(stroke.toPx(), cap = StrokeCap.Round)
            drawArc(Color.White.copy(alpha = 0.08f), 0f, 360f, false, style = st)
            drawArc(
                Brush.sweepGradient(listOf(Cyan, Neon, Violet, Cyan)),
                startAngle = -90f, sweepAngle = 360f * p, useCenter = false, style = st
            )
        }
        Text(label, color = SilverBright, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
    }
}

/** Round gradient avatar with the person's initials. */
@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier, ring: Color? = null) {
    val initials = name.trim().split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    Box(
        modifier
            .size(46.dp)
            .shadow(8.dp, CircleShape, ambientColor = ring ?: Neon, spotColor = ring ?: Neon)
            .background(Brush.linearGradient(listOf(Neon, Violet)), CircleShape)
            .border(2.dp, (ring ?: Color.White).copy(alpha = if (ring != null) 0.95f else 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text(initials, color = Color.White, fontWeight = FontWeight.Black) }
}

/** Small glass chip (used for group name and counters). */
@Composable
fun GlowChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) { Text(text, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
}
