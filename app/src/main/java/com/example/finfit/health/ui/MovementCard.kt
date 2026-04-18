package com.example.finfit.health.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

// ─── Màu sắc cho vòng tròn tiến trình ─────────────────────────
private val TrackColor = Color(0xFF262626) // surface_variant
private val ProgressBlue = Color(0xFFbbffb3) // tertiary (Activity)
private val OverflowRed = Color(0xFFff716c) // error

/**
 * CircularStepProgress — vẽ vòng tròn 3 lớp bằng Canvas.
 * Dùng chung cho cả StepCounterScreen và HealthDashboard.
 *
 * An toàn: stepGoal ≤ 0 → dùng 1 tránh chia 0.
 * Hiệu suất: KHÔNG tạo Paint/Path trong onDraw, chỉ dùng drawArc.
 */
@Composable
fun CircularStepProgress(
    currentSteps: Int,
    stepGoal: Int,
    modifier: Modifier = Modifier
) {
    val safeGoal = if (stepGoal <= 0) 1 else stepGoal
    val ratio = currentSteps.toFloat() / safeGoal.toFloat()

    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 600),
        label = "stepProgressAnim"
    )

    val strokeWidth = 16.dp

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val padding = strokeWidth.toPx() / 2f
            val arcSize = Size(size.width - strokeWidth.toPx(), size.height - strokeWidth.toPx())
            val topLeft = Offset(padding, padding)

            // Lớp 1: Track nền xám
            drawArc(TrackColor, 0f, 360f, false, topLeft, arcSize, style = stroke)

            // Lớp 2: Progress xanh (0→100%)
            val blueSweep = min(animatedRatio, 1f) * 360f
            if (blueSweep > 0f) {
                drawArc(ProgressBlue, 270f, blueSweep, false, topLeft, arcSize, style = stroke)
            }

            // Lớp 3: Overflow đỏ (>100%)
            if (animatedRatio > 1f) {
                val redSweep = (animatedRatio - 1f) * 360f
                drawArc(OverflowRed, 270f, min(redSweep, 360f), false, topLeft, arcSize, style = stroke)
            }
        }

        // Chữ trung tâm
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatStepNumber(currentSteps),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "/ ${formatStepNumber(safeGoal)}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatStepNumber(value: Int): String {
    return NumberFormat.getInstance(Locale("vi", "VN")).format(value)
}
