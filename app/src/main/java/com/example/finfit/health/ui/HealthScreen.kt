package com.example.finfit.health.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource

import com.example.finfit.health.repository.HealthViewModel
import com.example.finfit.ui.theme.PrimaryBlue
import com.example.finfit.core.navigation.Routes
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ─── Bubble Logic for Water Card ──────────────────────────────────
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
        radius = Random.nextFloat() * 10f + 5f // smaller for mini card
        speed = Random.nextFloat() * 10f + 5f
        alpha = Random.nextFloat() * 0.4f + 0.15f
        active = true
        needsReset = false
    }
    fun update() {
        if (!active || needsReset) return
        y -= speed
        if (y < -20f) {
            active = false
        }
    }
}

// ─── CÁC THÀNH PHẦN CHUNG ──────────────────────────────
@Composable
fun HealthHeaderSection(
    title: String,
    userEmail: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    actionIcon: ImageVector? = null,
    onActionClick: () -> Unit = {},
    actionIcon2: ImageVector? = null,
    onActionClick2: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262626)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFF64b5f6) // Primary / Water element
                    )
                }
            }
            Spacer(modifier = Modifier.width(if (showBackButton) 8.dp else 12.dp))
            Column {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "Chào buổi sáng, ${userEmail.split("@")[0]}",
                    color = Color(0xFFadaaaa),
                    fontSize = 12.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actionIcon2 != null) {
                IconButton(onClick = onActionClick2) {
                    Icon(
                        actionIcon2,
                        contentDescription = "Action 2",
                        tint = Color(0xFFff716c) // Error
                    )
                }
            }
            if (actionIcon != null) {
                IconButton(onClick = onActionClick) {
                    Icon(
                        actionIcon,
                        contentDescription = "Action",
                        tint = Color(0xFF64b5f6)
                    )
                }
            }

            // Các icon gốc luôn xuất hiện
            IconButton(onClick = onHomeClick) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White)
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF262626)))
        }
    }
}

// Common Placeholder Screen cho các module tính năng
@Composable
fun HealthPlaceholderScreen(
    userEmail: String, 
    title: String, 
    showBackButton: Boolean = true,
    onBack: () -> Unit = {}, 
    onHome: () -> Unit = onBack
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0e0e0e))) {
        HealthHeaderSection(
            title = title,
            userEmail = userEmail,
            showBackButton = showBackButton,
            onBackClick = onBack,
            onHomeClick = onHome
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tính năng đang phát triển",
                    fontSize = 16.sp,
                    color = Color(0xFFadaaaa)
                )
            }
        }
    }
}

@Composable fun HealthStatsScreen(userEmail: String, onBack: () -> Unit, onHome: () -> Unit = onBack) = HealthPlaceholderScreen(userEmail, "Phân tích sức khỏe", showBackButton = false, onBack = onBack, onHome = onHome)
@Composable fun HealthPredictionScreen(userEmail: String, onBack: () -> Unit, onHome: () -> Unit = onBack) = HealthPlaceholderScreen(userEmail, "Dự báo sức khỏe", onBack = onBack, onHome = onHome)
@Composable fun HealthLogScreen(userEmail: String, onBack: () -> Unit, onHome: () -> Unit = onBack) = HealthPlaceholderScreen(userEmail, "Nhật ký sức khỏe", onBack = onBack, onHome = onHome)

// Dữ liệu cho Card
data class HealthCardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

// ════════════════════════════════════════════════════════════════════════════
// MÀN HÌNH DASHBOARD SỨC KHỎE CHÍNH LAZY VERTICAL GRID
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun HealthDashboardScreen(
    userEmail: String,
    onNavigate: (String) -> Unit,
    healthViewModel: HealthViewModel = viewModel()
) {
    val userName = userEmail.split("@")[0]
    val uiState by healthViewModel.healthUiState.collectAsStateWithLifecycle()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0e0e0e))
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Greeting Section (Span 2) ---
        item(span = { GridItemSpan(2) }) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Chào buổi sáng.",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Bắt đầu ngày mới năng động và khỏe mạnh.",
                    fontSize = 16.sp,
                    color = Color(0xFFadaaaa)
                )
            }
        }

        // --- Water Card (Span 1) ---
        item(span = { GridItemSpan(1) }) {
            HealthWaterMiniCard(
                consumedMl = uiState.waterConsumedMl,
                goalMl = uiState.waterGoalMl,
                onAddWater = { amount -> healthViewModel.logWater(amountMl = amount, goalMl = uiState.waterGoalMl) },
                onClick = { onNavigate(Routes.WATER_TRACKER) }
            )
        }

        // --- Steps & Activity (Span 1) ---
        item(span = { GridItemSpan(1) }) {
            HealthStepActivityCard(
                steps = uiState.steps,
                stepGoal = uiState.stepGoal,
                caloriesOut = uiState.caloriesOut,
                onClick = { onNavigate("stepCounter") }
            )
        }

        // --- Energy Balance (Span 1) ---
        item(span = { GridItemSpan(1) }) {
            HealthEnergyBalanceCard(
                netCalorieBalance = uiState.netCalorieBalance
            )
        }

        // --- Sleep Tracking (Span 1) ---
        item(span = { GridItemSpan(1) }) {
            HealthSleepArcCard(
                sleepHours = uiState.sleepHours,
                onClick = { onNavigate(Routes.SLEEP_SCHEDULE) }
            )
        }

        // --- AI Food Scan Hành động (Span 2) ---
        item(span = { GridItemSpan(2) }) {
            HealthFoodScanCard(
                onClick = { onNavigate("food_scanner") }
            )
        }


    }
}

// ════════════════════════════════════════════════════════════════════════════
// CÁC THÀNH PHẦN GRID CARDS TINH CHỈNH THEO DESIGN SYSTEM
// (Màu nền 1a1a1a, Nút bấm 262626, Không viền, Text Trắng & adaauu)
// ════════════════════════════════════════════════════════════════════════════

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
    // UX Rule: Xanh dương khi chưa đạt, xanh lá khi đạt goal
    val goalReached = consumedMl >= goalMl && goalMl > 0
    val targetWaterColor = if (goalReached) Color(0xFF81C784) else Color(0xFF64B5F6)
    val waterColor by animateColorAsState(targetWaterColor, tween(600), label = "waterColorGoal")

    // Bubble management
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
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = waterColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nước", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFadaaaa))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Center Canvas (Human shape) + Bubbles overlay
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    animationFrame.value.let {
                        for (b in bubbles) {
                            if (b.active) {
                                if (b.needsReset) b.reset(size.width, size.height)
                                drawCircle(
                                    color = waterColor.copy(alpha = b.alpha),
                                    radius = b.radius,
                                    center = Offset(b.x, b.y)
                                )
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
            
            // Value
            Text(
                "${consumedMl}ml",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            
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

@Composable
fun HealthStepActivityCard(
    steps: Int,
    stepGoal: Int,
    caloriesOut: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(240.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DirectionsWalk, contentDescription = null, tint = Color(0xFFbbffb3), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Vận động", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFadaaaa))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularStepProgress(currentSteps = steps, stepGoal = stepGoal, modifier = Modifier.size(90.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Value
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    NumberFormat.getInstance(Locale("vi", "VN")).format(steps),
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Text(
                    " bước",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFFadaaaa),
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )
            }
            Text(
                "Đã đốt: $caloriesOut kcal",
                fontSize = 11.sp,
                color = Color(0xFFea73fb)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun HealthSleepArcCard(
    sleepHours: Float,
    onClick: () -> Unit
) {
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
                    if (sweep > 0) {
                        drawArc(sleepColor, 180f, sweep, false, Offset.Zero, arcSize, style = stroke)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${String.format("%.1f", sleepHours)}h",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = Color.White
                    )
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

@Composable
fun HealthEnergyBalanceCard(
    netCalorieBalance: Int
) {
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
            
            Text(
                "${Math.abs(netCalorieBalance)}",
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Text(
                "kcal $textState",
                fontSize = 14.sp,
                color = energyColor,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Nạp - Tiêu hao",
                fontSize = 11.sp,
                color = Color(0xFFadaaaa)
            )
        }
    }
}

@Composable
fun HealthFoodScanCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column {
            // Image section with FOOD SCAN chip overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Image(
                    painter = painterResource(id = com.example.finfit.R.drawable.foodscan),
                    contentDescription = "Food Scan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Glassmorphism chip
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "FOOD SCAN",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Bottom text and button section
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Ghi lại bữa ăn của bạn",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Phân tích calo và dinh dưỡng qua AI",
                        fontSize = 12.sp,
                        color = Color(0xFFadaaaa)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64b5f6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Bắt đầu", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0e0e0e))
                }
            }
        }
    }
}

@Composable
fun InsightItem(title: String, source: String, icon: ImageVector, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF262626), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = accentColor)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Text(text = source, fontSize = 11.sp, color = Color(0xFFadaaaa))
            }
        }
    }
}
