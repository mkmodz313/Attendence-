package com.attendance.pro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendance.pro.ui.theme.*

@Composable
fun SplashScreen() {
    val t = rememberInfiniteTransition(label = "pulse")
    val glow by t.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "g"
    )
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, tween(700), label = "a")

    Box(Modifier.fillMaxSize().background(BackgroundBrush), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(a)) {
            Box(
                Modifier.size(110.dp).scale(glow)
                    .shadow(28.dp, CircleShape, ambientColor = Silver, spotColor = Silver)
                    .background(Graphite, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.AccessTime, null, tint = SilverBright, modifier = Modifier.size(56.dp)) }
            Spacer(Modifier.height(24.dp))
            Text("ATTENDANCE PRO", color = SilverBright, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text("Workforce Management", color = Silver.copy(alpha = 0.7f), fontSize = 13.sp, letterSpacing = 2.sp)
        }
    }
}
