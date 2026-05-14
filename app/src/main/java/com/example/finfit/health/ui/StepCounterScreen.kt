package com.example.finfit.health.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finfit.health.model.HealthUiState
import com.example.finfit.health.repository.HealthViewModel
import com.example.finfit.health.repository.StepCounterService
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

// ─── Màu sắc ────────────────────────────────────────────────
private val TrackColor = Color(0xFF262626) // surface_variant
private val ProgressBlue = Color(0xFFbbffb3) // activity color
private val OverflowRed = Color(0xFFff716c) // error color
private val CalorieOrange = Color(0xFFF59E0B)
private val ActiveGreen = Color(0xFFbbffb3)

// ═══════════════════════════════════════════════════════════════
// StepCounterScreen — Màn hình chính hiển thị bước chân
// ═══════════════════════════════════════════════════════════════
@Composable
fun StepCounterScreen(
    userEmail: String,
    onBack: () -> Unit,
    healthViewModel: HealthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by healthViewModel.healthUiState.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasPermission = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: true
        }
    )

    var showWipeDialog by remember { mutableStateOf(false) }
    var showAchievement by remember { mutableStateOf(false) }

    // Fix lỗi cancel state: Dùng uiState.isFirst1000StepsAchieved làm key duy nhất để tránh LaunchedEffect bị hủy khi hasCelebrated đổi sang true
    LaunchedEffect(uiState.isFirst1000StepsAchieved) {
        if (uiState.isFirst1000StepsAchieved && !uiState.hasCelebrated1000Steps) {
            showAchievement = true
            healthViewModel.mark1000StepsCelebrated()
            delay(5000)
            showAchievement = false
        }
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && !StepCounterService.isRunning(context)) {
            StepCounterService.start(context)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0e0e0e) // dark background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            HealthHeaderSection(
                title = "Đếm bước chân",
                userEmail = userEmail,
                showBackButton = true,
                onBackClick = onBack,
                actionIcon = Icons.Rounded.Sync,
                onActionClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar("⏳ Đang đồng bộ...")
                    }
                    healthViewModel.forceSyncWithCallback { success ->
                        scope.launch {
                            if (success) {
                                snackbarHostState.showSnackbar("✅ Đồng bộ thành công!")
                            } else {
                                snackbarHostState.showSnackbar("❌ Lỗi mạng. Vui lòng thử lại sau!")
                            }
                        }
                    }
                },
                actionIcon2 = Icons.Rounded.Delete,
                onActionClick2 = { showWipeDialog = true }
            )

            if (hasPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // ─── Hàng 1: MainProgressCard (Full width) ───
                    MainProgressCard(uiState)

                    Spacer(Modifier.height(16.dp))

                    // ─── Hàng 2: MoveCard + ExerciseCard (2 cột) ───
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoveCard(
                            steps = uiState.steps,
                            calories = uiState.caloriesOut,
                            modifier = Modifier.weight(1f)
                        )
                        ExerciseCard(
                            activeMinutes = uiState.activeMinutes,
                            goalMinutes = uiState.activeMinuteGoal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ─── Hàng 3: GoalStatusCard (Full width) ───
                    GoalStatusCard(uiState)

                    Spacer(Modifier.height(16.dp))

                    // ─── Hàng 4: Thống kê trong ngày (Biểu đồ) ───
                    ActivityHistoryChart()

                    Spacer(Modifier.height(16.dp))
                    
                    // ─── Hàng 5: Upcoming Achievement ───
                    UpcomingAchievementCard(uiState)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vui lòng cấp quyền theo dõi vận động để sử dụng tính năng này",
                        modifier = Modifier.padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }

            if (showWipeDialog) {
                AlertDialog(
                    onDismissRequest = { showWipeDialog = false },
                    title = { Text("Xoá bước chân hôm nay") },
                    text = { Text("Bạn có chắc chắn muốn xóa bước chân ngày hôm nay? Dữ liệu nước uống sẽ được giữ nguyên.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showWipeDialog = false
                                healthViewModel.resetTodaySteps {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("🗑️ Đã xoá bước chân hôm nay!")
                                    }
                                }
                            }
                        ) {
                            Text("Xoá", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWipeDialog = false }) {
                            Text("Hủy")
                        }
                    }
                )
            }
        }
        
        FireworksOverlay(show = showAchievement)
        AchievementDialog(show = showAchievement)
    }
}

// ═══════════════════════════════════════════════════════════════
// MainProgressCard — Vòng tròn Canvas chính
// ═══════════════════════════════════════════════════════════════
@Composable
private fun MainProgressCard(state: HealthUiState) {
    val safeGoal = if (state.stepGoal <= 0) 1 else state.stepGoal
    val ratio = state.steps.toFloat() / safeGoal.toFloat()
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 600),
        label = "progressAnim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a1a)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Vòng tròn Canvas
            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                val strokeWidth = 16.dp
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    val pad = strokeWidth.toPx() / 2f
                    val arcSize = Size(size.width - strokeWidth.toPx(), size.height - strokeWidth.toPx())
                    val topLeft = Offset(pad, pad)

                    // Lớp 1: Track nền xám
                    drawArc(TrackColor, 0f, 360f, false, topLeft, arcSize, style = stroke)

                    // Lớp 2: Progress xanh (0→100%)
                    val blueSweep = min(animatedRatio, 1f) * 360f
                    if (blueSweep > 0f) {
                        drawArc(ProgressBlue, 270f, blueSweep, false, topLeft, arcSize, style = stroke)
                    }

                    // Lớp 3: Overflow đỏ (>100%)
                    if (animatedRatio > 1f) {
                        val redSweep = min((animatedRatio - 1f) * 360f, 360f)
                        drawArc(OverflowRed, 270f, redSweep, false, topLeft, arcSize, style = stroke)
                    }
                }

                // Chữ trung tâm
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatNumber(state.steps),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "/ ${formatNumber(safeGoal)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFadaaaa)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Activity Score",
                        fontSize = 10.sp,
                        color = Color(0xFFadaaaa)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MoveCard — Số bước + Calo
// ═══════════════════════════════════════════════════════════════
@Composable
private fun MoveCard(steps: Int, calories: Int, modifier: Modifier = Modifier) {
    StatCard(
        modifier = modifier,
        icon = Icons.Rounded.DirectionsWalk,
        iconTint = ProgressBlue,
        title = "Move",
        mainValue = formatNumber(steps),
        mainUnit = "bước",
        subLabel = "/ ${formatNumber(calories)} Kcal",
        subColor = CalorieOrange
    )
}

// ═══════════════════════════════════════════════════════════════
// ExerciseCard — Thời gian hoạt động
// ═══════════════════════════════════════════════════════════════
@Composable
private fun ExerciseCard(activeMinutes: Int, goalMinutes: Int, modifier: Modifier = Modifier) {
    StatCard(
        modifier = modifier,
        icon = Icons.Rounded.Schedule,
        iconTint = ActiveGreen,
        title = "Exercise",
        mainValue = "$activeMinutes",
        mainUnit = "phút",
        subLabel = "/ $goalMinutes Min",
        subColor = ActiveGreen
    )
}

// ═══════════════════════════════════════════════════════════════
// GoalStatusCard — % mục tiêu ngày
// ═══════════════════════════════════════════════════════════════
@Composable
private fun GoalStatusCard(state: HealthUiState) {
    val safeGoal = if (state.stepGoal <= 0) 1 else state.stepGoal
    val percentage = ((state.steps.toFloat() / safeGoal.toFloat()) * 100).toInt()
    val isCompleted = percentage >= 100

    val animatedProgress by animateFloatAsState(
        targetValue = min(state.steps.toFloat() / safeGoal.toFloat(), 1f),
        animationSpec = tween(durationMillis = 600),
        label = "goalBarAnim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a1a)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Flag,
                    contentDescription = null,
                    tint = if (isCompleted) OverflowRed else ProgressBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Mục tiêu ngày",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "$percentage%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (isCompleted) OverflowRed else ProgressBlue
                )
            }

            Spacer(Modifier.height(12.dp))

            // Thanh tiến trình ngang
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = if (isCompleted) OverflowRed else ProgressBlue,
                trackColor = TrackColor,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isCompleted) "🎉 Bạn đã vượt mục tiêu hôm nay!"
                       else "Còn ${formatNumber(safeGoal - state.steps)} bước nữa",
                fontSize = 12.sp,
                color = Color(0xFFadaaaa)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// StatCard — Card chung cho MoveCard & ExerciseCard
// ═══════════════════════════════════════════════════════════════
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    mainValue: String,
    mainUnit: String,
    subLabel: String,
    subColor: Color
) {
    Card(
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a1a)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFadaaaa))
            }

            Column {
                Text(
                    text = mainValue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = mainUnit,
                    fontSize = 11.sp,
                    color = Color(0xFFadaaaa)
                )
            }

            Text(
                text = subLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = subColor
            )
        }
    }
}

// ─── Thống kê trong ngày (Biểu đồ) ───────────────────────────────────────
@Composable
private fun ActivityHistoryChart() {
    // Render giả lập 24 cột dữ liệu vì hiện tại DB chưa track theo giờ
    val hourlyData = remember {
        List(24) { index ->
            when (index) {
                in 0..5 -> Random.nextInt(0, 10)
                in 6..8 -> Random.nextInt(50, 150)
                in 9..14 -> Random.nextInt(20, 100)
                in 15..18 -> Random.nextInt(100, 300)
                in 19..22 -> Random.nextInt(10, 50)
                else -> Random.nextInt(0, 5)
            }
        }
    }
    
    val maxStep = hourlyData.maxOrNull()?.coerceAtLeast(1) ?: 1
    
    Card(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Lịch sử hoạt động",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = "HÔM NAY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFadaaaa),
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Vẽ các đường Grid ngang mờ
                    val lines = 4
                    val lineSpacing = canvasHeight / lines
                    for (i in 0..lines) {
                        val y = i * lineSpacing
                        drawLine(
                            color = Color(0xFF262626), 
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 2f
                        )
                    }
                    
                    // Vẽ 24 cột
                    val barCount = 24
                    val gap = 8f
                    val totalGap = gap * (barCount - 1)
                    val barWidth = (canvasWidth - totalGap) / barCount
                    
                    hourlyData.forEachIndexed { index, value ->
                        val barHeight = (value.toFloat() / maxStep) * canvasHeight
                        val x = index * (barWidth + gap)
                        val y = canvasHeight - barHeight
                        
                        val isMax = value == maxStep
                        val barColor = if (isMax) Color(0xFF64b5f6) else Color(0xFF64b5f6).copy(alpha = 0.3f)
                        
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Text X-axis
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("00:00", fontSize = 12.sp, color = Color(0xFFadaaaa))
                Text("12:00", fontSize = 12.sp, color = Color(0xFFadaaaa))
                Text("23:00", fontSize = 12.sp, color = Color(0xFFadaaaa))
            }
        }
    }
}

// ─── Helper ────────────────────────────────────────────────────
private fun formatNumber(value: Int): String {
    return NumberFormat.getInstance(Locale("vi", "VN")).format(value)
}

@Composable
private fun UpcomingAchievementCard(state: HealthUiState) {
    if (state.isFirst1000StepsAchieved) return
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("SẮP ĐẠT ĐƯỢC", fontSize = 10.sp, color = Color(0xFF64b5f6), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text("Người mới bắt đầu", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Hoàn thành 1,000 bước đi tiên phong", fontSize = 12.sp, color = Color(0xFFadaaaa))
            }
            Icon(Icons.Rounded.StarBorder, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFF262626))
        }
    }
}

@Composable
fun FireworksOverlay(show: Boolean) {
    if (!show) return
    val particles = remember { List(100) { FireworksParticle() } }
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(show) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(3000))
    }
    
    Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val centerLeft = Offset(size.width * 0.2f, size.height * 0.6f)
            val centerRight = Offset(size.width * 0.8f, size.height * 0.6f)
            val currentProg = progress.value
            
            if (currentProg < 1f) {
                particles.forEach { p ->
                    val xL = centerLeft.x + kotlin.math.cos(p.angle) * p.distance * currentProg
                    val yL = centerLeft.y + kotlin.math.sin(p.angle) * p.distance * currentProg + (currentProg * currentProg * 200f) // gravity
                    drawCircle(p.color, p.size * (1f - currentProg), Offset(xL.toFloat(), yL.toFloat()))
                    
                    val xR = centerRight.x + kotlin.math.cos(p.angle) * p.distance * currentProg
                    val yR = centerRight.y + kotlin.math.sin(p.angle) * p.distance * currentProg + (currentProg * currentProg * 200f)
                    drawCircle(p.color, p.size * (1f - currentProg), Offset(xR.toFloat(), yR.toFloat()))
                }
            }
        }
    }
}

class FireworksParticle {
    val angle = Random.nextFloat() * 2 * kotlin.math.PI
    val distance = Random.nextFloat() * 500f + 100f
    val size = Random.nextFloat() * 8f + 4f
    val colors = listOf(Color(0xFFbbffb3), Color(0xFF64b5f6), Color.White)
    val color = colors.random()
}

@Composable
fun AchievementDialog(show: Boolean) {
    if (!show) return
    Box(modifier = Modifier.fillMaxSize().zIndex(100f), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a)),
            modifier = Modifier.padding(32.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Chúc mừng!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                Spacer(Modifier.height(8.dp))
                Text("Bạn đã hoàn thành 1,000 bước đi đầu tiên. Tuyệt vời!", fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
            }
        }
    }
}
