package com.example.finfit.health.ui.cards

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.health.model.HealthUiState
import com.example.finfit.health.ui.CircularStepProgress
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.min
import kotlin.random.Random

// ─── Bubble Logic for Water Card ──────────────────────────────────────────────

private class MiniWaterBubble(
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 0f,
    var speed: Float = 0f,
    var alpha: Float = 0f,
    var active: Boolean = false,
    var needsReset: Boolean = true
) {
    fun reset(canvasWidth: Float, canvasHeight: Float) {
        x = Random.nextFloat() * canvasWidth
        y = canvasHeight + Random.nextFloat() * 20f
        radius = Random.nextFloat() * 10f + 5f
        speed = Random.nextFloat() * 10f + 5f
        alpha = Random.nextFloat() * 0.4f + 0.15f
        active = true
        needsReset = false
    }
    fun update() {
        if (!active || needsReset) return
        y -= speed
        if (y < -20f) active = false
    }
}

// ─── Water Mini Card ───────────────────────────────────────────────────────────

@Composable
fun HealthWaterMiniCard(
    consumedMl: Int,
    goalMl: Int,
    onAddWater: (Int) -> Unit,
    onClick: () -> Unit
) {
    val safeGoal = if (goalMl <= 0) 1 else goalMl
    val percentage = consumedMl.toFloat() / safeGoal.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = minOf(percentage, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "waterFillAnimMini"
    )
    val goalReached = consumedMl >= goalMl && goalMl > 0
    val targetWaterColor = if (goalReached) Color(0xFF81C784) else Color(0xFF64B5F6)
    val waterColor by animateColorAsState(targetWaterColor, tween(600), label = "waterColorGoal")

    val maxBubbles = 20
    val bubbles = remember { List(maxBubbles) { MiniWaterBubble() } }
    var previousConsumed by remember { mutableIntStateOf(consumedMl) }

    LaunchedEffect(consumedMl) {
        if (consumedMl > previousConsumed) {
            var spawned = 0
            for (bubble in bubbles) {
                if (!bubble.active) {
                    bubble.active = true
                    bubble.needsReset = true
                    spawned++
                    if (spawned >= 10) break
                }
            }
        }
        previousConsumed = consumedMl
    }

    val animationFrame = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time ->
                animationFrame.value = time
                for (b in bubbles) { if (b.active) b.update() }
            }
        }
    }

    val humanPath = remember { Path() }
    val wavePath = remember { Path() }
    val infiniteTransition = rememberInfiniteTransition(label = "waveInfinite")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "wavePhase"
    )

    Card(
        modifier = Modifier.fillMaxWidth().height(240.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = waterColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nước", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFadaaaa))
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    animationFrame.value.let {
                        for (b in bubbles) {
                            if (b.active) {
                                if (b.needsReset) b.reset(size.width, size.height)
                                drawCircle(color = waterColor.copy(alpha = b.alpha), radius = b.radius, center = Offset(b.x, b.y))
                            }
                        }
                    }
                }
                Canvas(modifier = Modifier.size(45.dp, 90.dp)) {
                    val scaleX = size.width / 64f
                    val scaleY = size.height / 128f
                    if (humanPath.isEmpty) {
                        humanPath.apply {
                            moveTo(32f, 4f); cubicTo(38.6f, 4f, 44f, 9.4f, 44f, 16f)
                            cubicTo(44f, 22.6f, 38.6f, 28f, 32f, 28f); cubicTo(25.4f, 28f, 20f, 22.6f, 20f, 16f)
                            cubicTo(20f, 9.4f, 25.4f, 4f, 32f, 4f); close()
                            moveTo(32f, 32f); cubicTo(20f, 32f, 13f, 34f, 12f, 44f); lineTo(8f, 76f)
                            cubicTo(8f, 80f, 12f, 80f, 14f, 78f); lineTo(18f, 50f); lineTo(20f, 80f)
                            lineTo(18f, 120f); cubicTo(18f, 124f, 26f, 124f, 26f, 120f); lineTo(30f, 84f)
                            cubicTo(31f, 80f, 33f, 80f, 34f, 84f); lineTo(38f, 120f)
                            cubicTo(38f, 124f, 46f, 124f, 46f, 120f); lineTo(44f, 80f)
                            lineTo(46f, 50f); lineTo(50f, 78f); cubicTo(52f, 80f, 56f, 80f, 56f, 76f)
                            lineTo(52f, 44f); cubicTo(51f, 34f, 44f, 32f, 32f, 32f); close()
                        }
                    }
                    withTransform({ scale(scaleX, scaleY, pivot = Offset.Zero) }) {
                        drawPath(humanPath, color = Color(0xFF262626))
                        clipPath(humanPath) {
                            val fillH = 128f * (1f - animatedProgress)
                            wavePath.reset(); wavePath.moveTo(0f, fillH)
                            if (animatedProgress > 0f) {
                                for (x in 0..64 step 4) wavePath.lineTo(x.toFloat(), fillH + sin((x / 32f) * 2 * PI + wavePhase).toFloat() * 2f)
                            } else wavePath.lineTo(64f, fillH)
                            wavePath.lineTo(64f, 128f); wavePath.lineTo(0f, 128f); wavePath.close()
                            drawPath(wavePath, color = waterColor)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("${consumedMl}ml", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White, letterSpacing = (-1).sp)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAddWater(200) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) { Text("+200", fontSize = 10.sp, color = waterColor, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { onAddWater(500) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) { Text("+500", fontSize = 10.sp, color = waterColor, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ─── Step & Activity Card ──────────────────────────────────────────────────────

@Composable
fun HealthStepActivityCard(steps: Int, stepGoal: Int, caloriesOut: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(240.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, contentDescription = null, tint = Color(0xFFbbffb3), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Vận động", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFadaaaa))
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularStepProgress(currentSteps = steps, stepGoal = stepGoal, modifier = Modifier.size(90.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    NumberFormat.getInstance(Locale("vi", "VN")).format(steps),
                    fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White, letterSpacing = (-1).sp
                )
                Text(" bước", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color(0xFFadaaaa), modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
            }
            Text("Đã đốt: $caloriesOut kcal", fontSize = 11.sp, color = Color(0xFFea73fb))
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Sleep Arc Card ────────────────────────────────────────────────────────────

@Composable
fun HealthSleepArcCard(sleepHours: Float, onClick: () -> Unit) {
    val sleepColor = Color(0xFFea73fb)
    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Nightlight, contentDescription = null, tint = sleepColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Giấc ngủ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFadaaaa))
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(70.dp).clipToBounds(), contentAlignment = Alignment.BottomCenter) {
                Canvas(modifier = Modifier.size(120.dp, 120.dp)) {
                    val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    val arcSize = Size(size.width, size.height)
                    drawArc(Color(0xFF262626), 180f, 180f, false, Offset.Zero, arcSize, style = stroke)
                    val sweep = (sleepHours / 8f).coerceIn(0f, 1f) * 180f
                    if (sweep > 0) drawArc(sleepColor, 180f, sweep, false, Offset.Zero, arcSize, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${String.format("%.1f", sleepHours)}h", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("23:00", fontSize = 10.sp, color = Color(0xFFadaaaa))
                Text("Mục tiêu 8h", fontSize = 10.sp, color = sleepColor, fontWeight = FontWeight.Bold)
                Text("07:00", fontSize = 10.sp, color = Color(0xFFadaaaa))
            }
        }
    }
}

// ─── Energy Balance Card ───────────────────────────────────────────────────────

@Composable
fun HealthEnergyBalanceCard(netCalorieBalance: Int) {
    val energyColor = if (netCalorieBalance > 0) Color(0xFFff716c) else Color(0xFFbbffb3)
    val textState = if (netCalorieBalance > 0) "Dư thừa" else "Thâm hụt"
    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MonitorWeight, contentDescription = null, tint = energyColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Cân bằng Calo", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFadaaaa))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("${Math.abs(netCalorieBalance)}", fontWeight = FontWeight.Black, fontSize = 36.sp, color = Color.White, letterSpacing = (-1).sp)
            Text("kcal $textState", fontSize = 14.sp, color = energyColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text("Nạp - Tiêu hao", fontSize = 11.sp, color = Color(0xFFadaaaa))
        }
    }
}

// ─── Food Scan Card ────────────────────────────────────────────────────────────

@Composable
fun HealthFoodScanCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                Image(
                    painter = painterResource(id = com.example.finfit.R.drawable.foodscan),
                    contentDescription = "Food Scan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("FOOD SCAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ghi lại bữa ăn của bạn", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Phân tích calo và dinh dưỡng qua AI", fontSize = 12.sp, color = Color(0xFFadaaaa))
                }
                Spacer(Modifier.width(16.dp))
                Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64b5f6)), shape = RoundedCornerShape(12.dp)) {
                    Text("Bắt đầu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0e0e0e))
                }
            }
        }
    }
}

// ─── Daily Health Score Card ───────────────────────────────────────────────────

@Composable
fun HealthDailyScoreCard(uiState: HealthUiState) {
    val totalScore = uiState.totalHealthScore
    val scoreText = when {
        totalScore >= 90 -> "Rất Tốt"
        totalScore >= 70 -> "Tốt"
        totalScore >= 50 -> "Trung bình"
        else -> "Cần cố gắng"
    }
    val scoreColor = when {
        totalScore >= 90 -> Color(0xFF64b5f6)
        totalScore >= 70 -> Color(0xFF81C784)
        totalScore >= 50 -> Color(0xFFFFC107)
        else -> Color(0xFFff716c)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = Color(0xFF262626), strokeWidth = 6.dp)
                    CircularProgressIndicator(
                        progress = { totalScore / 100f }, modifier = Modifier.fillMaxSize(),
                        color = scoreColor, strokeWidth = 6.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(text = "$totalScore", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(text = scoreText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                    Text(text = "$totalScore/100", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "(Dựa trên 4 chỉ số bên dưới)", fontSize = 12.sp, color = Color(0xFFadaaaa))
                }
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.HorizontalDivider(color = Color(0xFF262626))
            Spacer(Modifier.height(16.dp))
            val formatStr = java.text.NumberFormat.getInstance(java.util.Locale("vi", "VN"))
            ScoreMetricRow("Dinh dưỡng", "${formatStr.format(uiState.caloriesIn)} / ${formatStr.format(uiState.calorieGoal)} kcal", "${uiState.nutritionScore}/30", Color(0xFFffcc80))
            ScoreMetricRow("Nước uống", "${formatStr.format(uiState.waterConsumedMl)} / ${formatStr.format(uiState.waterGoalMl)} ml", "${uiState.waterScore}/20", Color(0xFF64b5f6))
            val h = uiState.sleepHours.toInt()
            val m = ((uiState.sleepHours - h) * 60).toInt()
            ScoreMetricRow("Giấc ngủ", "${h}h ${m}m / 8h", "${uiState.sleepScore}/25", Color(0xFFea73fb))
            ScoreMetricRow("Vận động", "${formatStr.format(uiState.steps)} / ${formatStr.format(uiState.stepGoal)} bước", "${uiState.activityScore}/25", Color(0xFFbbffb3))
        }
    }
}

@Composable
private fun ScoreMetricRow(label: String, value: String, scoreStr: String, dotColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(value, fontSize = 14.sp, color = Color(0xFFadaaaa))
        }
        Text(scoreStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ─── Insight Item ──────────────────────────────────────────────────────────────

@Composable
fun InsightItem(title: String, source: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accentColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(64.dp).background(Color(0xFF262626), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = accentColor)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Text(text = source, fontSize = 11.sp, color = Color(0xFFadaaaa))
            }
        }
    }
}

@Composable
fun FinanceSummaryMiniCard(
    wallet: com.example.finfit.finance.model.AppUserWallet?,
    transactions: List<com.example.finfit.finance.model.FinanceTransaction>,
    goals: List<com.example.finfit.finance.model.SavingsGoal>,
    onClick: () -> Unit
) {
    val spendable = remember(wallet, goals) {
        if (wallet == null) 0.0 else {
            val personal = wallet.totalBalance
            val goal = goals.sumOf { it.currentAmount }
            val general = wallet.generalSavings
            (personal - goal - general).coerceAtLeast(0.0)
        }
    }

    val todayFoodSpent = remember(transactions) {
        transactions.filter { tx ->
            val cal = java.util.Calendar.getInstance().apply { time = tx.timestamp.toDate() }
            val today = java.util.Calendar.getInstance()
            cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) &&
            tx.type == com.example.finfit.finance.model.TransactionType.EXPENSE &&
            tx.category == "Ăn uống"
        }.sumOf { it.amount }
    }

    val vndFormat = remember { java.text.NumberFormat.getInstance(java.util.Locale("vi", "VN")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Tài chính hôm nay",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFadaaaa)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Số dư khả dụng",
                        fontSize = 12.sp,
                        color = Color(0xFFadaaaa)
                    )
                    Text(
                        text = "${vndFormat.format(spendable)}đ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Chi ăn uống hôm nay",
                        fontSize = 12.sp,
                        color = Color(0xFFadaaaa)
                    )
                    Text(
                        text = "${vndFormat.format(todayFoodSpent)}đ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFff716c)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.HorizontalDivider(color = Color(0xFF262626))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quản lý tài chính",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64b5f6)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF64b5f6),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
