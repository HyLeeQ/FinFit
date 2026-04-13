package com.example.finfit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var animateStart by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateStart = true
        delay(2200) // 2.2 seconds display
        onSplashFinished()
    }

    val scale by animateFloatAsState(
        targetValue = if (animateStart) 1.2f else 0.5f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "LogoScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (animateStart) 1f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "LogoAlpha"
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (animateStart) 0.7f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 600),
        label = "SubtitleAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF14151B),
                        Color(0xFF0A0B10)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .scale(scale)
                    .alpha(alpha)
            ) {
                // Thử hiển thị Logo hoặc Icon nếu có, nếu không thì dùng chữ FinFit cách điệu vàng.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "FinFit",
                        color = Color(0xFFEAB308),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .background(Color(0xFFEAB308), shape = androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Quản lý tài chính cá nhân",
                color = Color.White.copy(alpha = subtitleAlpha),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(subtitleAlpha)
            )
        }
    }
}
