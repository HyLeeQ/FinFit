package com.example.finfit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SplashScreen chạy song song:
 *   1. Animation logo (tối thiểu MIN_SHOW_MS)
 *   2. Prefetch dữ liệu Firestore qua [onPreload]
 * Chỉ navigate khi CẢ HAI đều xong.
 *
 * @param onPreload  suspend lambda gọi Firestore; trả về khi data sẵn sàng
 * @param onSplashFinished  callback điều hướng sang màn hình tiếp theo
 */
@Composable
fun SplashScreen(
    onPreload: suspend () -> Unit = {},
    onSplashFinished: () -> Unit
) {
    val MIN_SHOW_MS = 2400L

    // State điều khiển animation
    var animIn    by remember { mutableStateOf(false) }
    var dataReady by remember { mutableStateOf(false) }
    var minDone   by remember { mutableStateOf(false) }

    // Chạy song song: min timer + preload
    LaunchedEffect(Unit) {
        animIn = true
        // Nhánh 1: đếm thời gian tối thiểu
        launch {
            delay(MIN_SHOW_MS)
            minDone = true
        }
        // Nhánh 2: prefetch data
        launch {
            try { onPreload() } catch (_: Exception) {}
            dataReady = true
        }
    }

    // Khi cả 2 xong → navigate
    LaunchedEffect(dataReady, minDone) {
        if (dataReady && minDone) onSplashFinished()
    }

    // ── Animations ─────────────────────────────────────────────────────────
    val logoScale by animateFloatAsState(
        targetValue = if (animIn) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (animIn) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (animIn) 0.75f else 0f,
        animationSpec = tween(600, delayMillis = 500),
        label = "subAlpha"
    )

    // ── Layout ─────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F1117), Color(0xFF0D0F1A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow phía sau logo
        Box(
            modifier = Modifier
                .size(280.dp)
                .alpha(logoAlpha * 0.35f)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF6366F1), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo card ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Icon box gradient
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💎", fontSize = 40.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    // Tên app gradient
                    Text(
                        text = "FinFit",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF6366F1), Color(0xFFA78BFA), Color(0xFF60A5FA))
                            )
                        )
                    )

                    Spacer(Modifier.height(6.dp))

                    // Tagline
                    Text(
                        text = "Tài chính · Sức khỏe · AI",
                        color = Color.White.copy(alpha = subtitleAlpha),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(Modifier.height(72.dp))

            // ── Loading indicator ──────────────────────────────────────
            LoadingDots(alpha = subtitleAlpha)
        }
    }
}

/** Ba chấm loading nhảy lần lượt */
@Composable
private fun LoadingDots(alpha: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotColors = listOf(Color(0xFF6366F1), Color(0xFFA78BFA), Color(0xFF60A5FA))

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.alpha(alpha)
    ) {
        dotColors.forEachIndexed { i, color ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue  = -10f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(400, delayMillis = i * 140, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
