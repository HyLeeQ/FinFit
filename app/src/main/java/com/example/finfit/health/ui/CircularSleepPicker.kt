package com.example.finfit.health.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun CircularSleepPicker(
    bedTimeMin: Int,
    wakeTimeMin: Int,
    onBedTimeChange: (Int) -> Unit,
    onWakeTimeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var draggingHandle by remember { mutableStateOf<String?>(null) }

    // Calculate duration
    val durationMin = if (wakeTimeMin >= bedTimeMin) wakeTimeMin - bedTimeMin else 1440 - bedTimeMin + wakeTimeMin
    val h = durationMin / 60
    val m = durationMin % 60

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f
                            
                            // Calculate handle positions
                            val bedAngle = (bedTimeMin / 1440f * 360f - 90f) * PI / 180f
                            val wakeAngle = (wakeTimeMin / 1440f * 360f - 90f) * PI / 180f
                            
                            val bedPos = Offset(
                                center.x + radius * cos(bedAngle).toFloat(),
                                center.y + radius * sin(bedAngle).toFloat()
                            )
                            val wakePos = Offset(
                                center.x + radius * cos(wakeAngle).toFloat(),
                                center.y + radius * sin(wakeAngle).toFloat()
                            )

                            val distToBed = sqrt((offset.x - bedPos.x).pow(2) + (offset.y - bedPos.y).pow(2))
                            val distToWake = sqrt((offset.x - wakePos.x).pow(2) + (offset.y - wakePos.y).pow(2))

                            // Hit target radius ~ 40dp
                            val hitRadius = with(density) { 40.dp.toPx() }
                            
                            if (distToBed < hitRadius && distToBed <= distToWake) {
                                draggingHandle = "bed"
                            } else if (distToWake < hitRadius) {
                                draggingHandle = "wake"
                            } else {
                                // Maybe dragging the arc itself (not implemented here)
                                draggingHandle = null
                            }
                        },
                        onDragEnd = { draggingHandle = null },
                        onDragCancel = { draggingHandle = null },
                        onDrag = { change, _ ->
                            change.consume()
                            if (draggingHandle != null) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val touchAngle = atan2(change.position.y - center.y, change.position.x - center.x) * 180 / PI
                                val normalizedAngle = (touchAngle + 360) % 360
                                val clockAngle = (normalizedAngle + 90) % 360
                                val newMin = ((clockAngle / 360f) * 1440f).roundToInt() % 1440
                                
                                // Snap to 5-minute intervals
                                val snappedMin = (newMin / 5) * 5
                                
                                if (draggingHandle == "bed") {
                                    onBedTimeChange(snappedMin)
                                } else if (draggingHandle == "wake") {
                                    onWakeTimeChange(snappedMin)
                                }
                            }
                        }
                    )
                }
        ) {
            val strokeWidth = 32.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f - strokeWidth / 2f

            // Draw background track
            drawCircle(
                color = Color(0xFF262626),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Draw hour ticks
            for (i in 0 until 24) {
                val angle = (i * 15f - 90f) * PI / 180f
                val innerRadius = radius - strokeWidth / 2f - 10.dp.toPx()
                val outerRadius = radius - strokeWidth / 2f - 2.dp.toPx()
                
                val start = Offset(
                    center.x + innerRadius * cos(angle).toFloat(),
                    center.y + innerRadius * sin(angle).toFloat()
                )
                val end = Offset(
                    center.x + outerRadius * cos(angle).toFloat(),
                    center.y + outerRadius * sin(angle).toFloat()
                )
                
                drawLine(
                    color = if (i % 6 == 0) Color(0xFFadaaaa) else Color(0xFF404040),
                    start = start,
                    end = end,
                    strokeWidth = if (i % 6 == 0) 4.dp.toPx() else 2.dp.toPx()
                )
            }

            // Draw active sleep arc
            val startAngle = (bedTimeMin / 1440f * 360f) - 90f
            val sweepAngle = (durationMin / 1440f * 360f)
            
            drawArc(
                color = Color(0xFF64b5f6),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(size.width - strokeWidth, size.height - strokeWidth)
            )

            // Draw Bed Handle indicator
            val bedAngleRad = startAngle * PI / 180f
            val bedHandlePos = Offset(
                center.x + radius * cos(bedAngleRad).toFloat(),
                center.y + radius * sin(bedAngleRad).toFloat()
            )
            drawCircle(
                color = Color.White,
                radius = strokeWidth / 2f - 4.dp.toPx(),
                center = bedHandlePos
            )

            // Draw Wake Handle indicator
            val wakeAngleRad = (startAngle + sweepAngle) * PI / 180f
            val wakeHandlePos = Offset(
                center.x + radius * cos(wakeAngleRad).toFloat(),
                center.y + radius * sin(wakeAngleRad).toFloat()
            )
            drawCircle(
                color = Color.White,
                radius = strokeWidth / 2f - 4.dp.toPx(),
                center = wakeHandlePos
            )
        }

        // Center text (Duration)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${h}hr ${m}min", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Thời gian ngủ", fontSize = 12.sp, color = Color(0xFFadaaaa))
        }
    }
}
