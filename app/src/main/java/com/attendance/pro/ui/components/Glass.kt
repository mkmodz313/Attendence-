package com.attendance.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendance.pro.ui.theme.*

private val CardShape = RoundedCornerShape(22.dp)
private val GlassFill = Brush.verticalGradient(
    listOf(Color(0xFF2B2D35).copy(alpha = 0.92f), Color(0xFF17181D).copy(alpha = 0.92f))
)
private val GlassBorder = Brush.linearGradient(
    listOf(Silver.copy(alpha = 0.55f), Color.Transparent, Silver.copy(alpha = 0.25f))
)

@Composable
fun GlassCard(modifier: Modifier = Modifier, accent: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .shadow(14.dp, CardShape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(GlassFill, CardShape)
            .then(if (accent != null) Modifier.border(1.5.dp, accent, CardShape) else Modifier.border(1.dp, GlassBorder, CardShape))
            .padding(18.dp),
        content = content
    )
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Silver, contentColor = Black,
            disabledContainerColor = Slate, disabledContentColor = Silver.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, SilverBright.copy(alpha = 0.6f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Silver)
        else Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Box(
        Modifier.background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}
