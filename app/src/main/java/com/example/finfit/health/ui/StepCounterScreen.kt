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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
private val TrackColor = Color.LightGray.copy(alpha = 0.3f)
private val ProgressBlue = Color(0xFF2196F3)
private val OverflowRed = Color(0xFFF44336)
private val CalorieOrange = Color(0xFFF59E0B)
private val ActiveGreen = Color(0xFF22C55E)

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
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            HealthHeaderSection(
                title = "Đếm bước chân",
                userEmail = userEmail,
                showBackButton = true,
                onBackClick = onBack,
                actionIcon = Icons.Rounded.Sync,
                onActionClick = {
                    healthViewModel.forceSyncWithCallback {
                        scope.launch {
                            snackbarHostState.showSnackbar("✅ Đồng bộ thành công!")
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
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vui lòng cấp quyền theo dõi vận động để sử dụng tính năng này",
                        modifier = Modifier.padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "/ ${formatNumber(safeGoal)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Activity Score",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                    color = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column {
                Text(
                    text = mainValue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = mainUnit,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

// ─── Helper ────────────────────────────────────────────────────
private fun formatNumber(value: Int): String {
    return NumberFormat.getInstance(Locale("vi", "VN")).format(value)
}
