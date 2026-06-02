package com.example.finfit.health.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finfit.health.model.DrinkType
import com.example.finfit.health.model.WaterLogUiItem
import com.example.finfit.health.model.WaterScreenData
import com.example.finfit.health.model.WaterUiState
import com.example.finfit.health.repository.HealthViewModel
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ====================================================================
// DRINK TYPE HELPERS — Màu sắc và icon cho từng loại thức uống
// ====================================================================

private data class DrinkMeta(val color: Color, val icon: ImageVector, val label: String)

private fun drinkMeta(drinkType: String): DrinkMeta = when (drinkType) {
    DrinkType.WATER  -> DrinkMeta(Color(0xFF64B5F6), Icons.Rounded.WaterDrop,        "Nước lọc")
    DrinkType.COFFEE -> DrinkMeta(Color(0xFF8D6E63), Icons.Rounded.Coffee,            "Cà phê")
    DrinkType.TEA    -> DrinkMeta(Color(0xFF81C784), Icons.Rounded.EmojiFoodBeverage, "Trà")
    DrinkType.MILK   -> DrinkMeta(Color(0xFFE0E0E0), Icons.Rounded.LocalDrink,        "Sữa")
    DrinkType.JUICE  -> DrinkMeta(Color(0xFFFFB74D), Icons.Rounded.Opacity,            "Nước ép")
    DrinkType.SODA   -> DrinkMeta(Color(0xFFBA68C8), Icons.Rounded.Star,              "Nước ngọt")
    else             -> DrinkMeta(Color(0xFF90A4AE), Icons.Rounded.Add,               "Khác")
}

// Danh sách preset hiển thị trong LazyRow
private data class DrinkPreset(val drinkType: String, val defaultAmounts: List<Int>)

private val DRINK_PRESETS = listOf(
    DrinkPreset(DrinkType.WATER,  listOf(150, 200, 300, 500)),
    DrinkPreset(DrinkType.TEA,    listOf(150, 200, 300)),
    DrinkPreset(DrinkType.COFFEE, listOf(150, 200, 250)),
    DrinkPreset(DrinkType.MILK,   listOf(200, 250)),
    DrinkPreset(DrinkType.JUICE,  listOf(150, 200, 300)),
    DrinkPreset(DrinkType.SODA,   listOf(200, 330)),
    DrinkPreset(DrinkType.OTHER,  listOf(200))
)

// ====================================================================
// BUBBLE ENGINE (giữ nguyên từ bản cũ)
// ====================================================================

private class WaterBubble(
    var x: Float = 0f, var y: Float = 0f, var radius: Float = 0f,
    var speed: Float = 0f, var alpha: Float = 0f,
    var active: Boolean = false, var needsReset: Boolean = true
) {
    fun reset(w: Float, h: Float) {
        x = Random.nextFloat() * w; y = h + Random.nextFloat() * 50f
        radius = Random.nextFloat() * 20f + 10f; speed = Random.nextFloat() * 15f + 10f
        alpha = Random.nextFloat() * 0.4f + 0.15f; active = true; needsReset = false
    }
    fun update() { if (!active || needsReset) return; y -= speed; if (y < -100f) active = false }
}

// ====================================================================
// MAIN SCREEN — subscribe waterUiState
// ====================================================================

@Composable
fun WaterTrackerScreen(
    userEmail: String,
    onBack: () -> Unit,
    healthViewModel: HealthViewModel = viewModel()
) {
    val waterUiState by healthViewModel.waterUiState.collectAsStateWithLifecycle()

    // Bubble pool
    val bubbles = remember { List(80) { WaterBubble() } }
    var previousConsumed by remember { mutableIntStateOf(0) }

    // Bubble spawn khi consumed tăng
    val consumed = (waterUiState as? WaterUiState.Ready)?.data?.consumedMl ?: 0
    LaunchedEffect(consumed) {
        if (consumed > previousConsumed) {
            var spawned = 0
            for (b in bubbles) { if (!b.active) { b.active = true; b.needsReset = true; spawned++; if (spawned >= 40) break } }
        }
        previousConsumed = consumed
    }

    val animationFrame = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) { withFrameNanos { t -> animationFrame.value = t; bubbles.forEach { if (it.active) it.update() } } }
    }

    val bubbleColor by animateColorAsState(Color(0xFF64B5F6), tween(500), label = "bubble")

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
            HealthHeaderSection(
                title = "Theo dõi uống nước",
                userEmail = userEmail,
                showBackButton = true,
                onBackClick = onBack
            )
            when (val state = waterUiState) {
                is WaterUiState.Loading -> WaterLoadingState()
                is WaterUiState.Error   -> WaterErrorState(state.message)
                is WaterUiState.Ready   -> WaterReadyContent(
                    data       = state.data,
                    onLogWater = { amount, drinkType ->
                        healthViewModel.logWater(
                            amountMl  = amount,
                            drinkType = drinkType,
                            goalMl    = state.data.goalMl
                        )
                    },
                    onDelete   = { logId ->
                        healthViewModel.deleteWaterLog(logId, state.data.goalMl)
                    },
                    onToggleReminder = { enabled ->
                        healthViewModel.toggleWaterReminder(enabled)
                    }
                )
            }
        }

        // Bubble overlay
        Canvas(Modifier.fillMaxSize()) {
            animationFrame.value.let {
                bubbles.forEach { b ->
                    if (b.active) {
                        if (b.needsReset) b.reset(size.width, size.height)
                        drawCircle(bubbleColor.copy(alpha = b.alpha), b.radius * 2f,
                            androidx.compose.ui.geometry.Offset(b.x, b.y))
                    }
                }
            }
        }
    }
}

// ====================================================================
// LOADING STATE
// ====================================================================

@Composable
private fun WaterLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color(0xFF64B5F6))
    }
}

// ====================================================================
// ERROR STATE
// ====================================================================

@Composable
private fun WaterErrorState(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Warning, null, tint = Color(0xFFEF5350), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color(0xFFADAAAA), textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}

// ====================================================================
// CAFFEINE MARQUEE TICKER
// ====================================================================

/** Enum mức Caffeine — dùng để vẽ UI đúng theo ngưỡng */
private enum class CaffeineLevel { SAFE, WARN, DANGER }

private fun caffeineLevel(mg: Int): CaffeineLevel = when {
    mg >= DrinkType.CAFFEINE_DANGER_MG -> CaffeineLevel.DANGER
    mg >= DrinkType.CAFFEINE_WARN_MG   -> CaffeineLevel.WARN
    else                               -> CaffeineLevel.SAFE
}

/**
 * Marquee ticker chạy từ phải sang trái.
 * Hiện thị khi Caffeine ≥ CAFFEINE_WARN_MG (200mg).
 *
 * @param caffeineMg Tổng caffeine hôm nay (mg)
 * @param dominantSource Loại đồ uống đóng góp caffeine nhiều nhất (DrinkType constant)
 *
 * Thiết kế cho Redmi 10 (360dp width, font 12sp):
 * - Text ≤ 52 ký tự → đọc được toàn bộ trong 1 chu kỳ.
 * - Tốc độ 72px/s — nhanh, rõ ràng, không gây mỏi mắt.
 */
@Composable
private fun CaffeineMarquee(caffeineMg: Int, dominantSource: String = DrinkType.COFFEE) {
    val level = caffeineLevel(caffeineMg)
    if (level == CaffeineLevel.SAFE) return

    val bgColor  = if (level == CaffeineLevel.DANGER) Color(0xFF7F1D1D) else Color(0xFF78350F)
    val txtColor = if (level == CaffeineLevel.DANGER) Color(0xFFFF6B6B) else Color(0xFFFBBF24)

    // Text khác nhau theo nguồn caffeine chính — Cà phê vs Trà ≠ nhau
    val message = when {
        level == CaffeineLevel.DANGER && dominantSource == DrinkType.TEA ->
            "⚠️ Trà tích lũy ${caffeineMg}mg! Chuyển sang nước lọc ngay."
        level == CaffeineLevel.DANGER ->
            "⚠️ Caffeine ${caffeineMg}mg/400mg! Dừng cà phê, uống nước lọc."
        dominantSource == DrinkType.TEA ->
            "🍵 Trà: ${caffeineMg}mg caffeine. Hãy thay bằng nước lọc."
        else ->
            "☕ Cà phê: ${caffeineMg}mg caffeine. Cân nhắc uống nước lọc."
    }

    val density = LocalDensity.current
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var textWidthPx       by remember { mutableIntStateOf(0) }

    // Tổng quãng đường = chiều rộng màn hình + chiều dài text
    val totalTravelPx = containerWidthPx + textWidthPx

    // Tốc độ 72px/s — tăng từ 42px/s, đủ nhanh mà vẫn đọc được
    val durationMs = if (totalTravelPx > 0) ((totalTravelPx / 72f) * 1000).toInt().coerceAtLeast(2500) else 4000

    val infiniteTransition = rememberInfiniteTransition(label = "cafMarquee")
    val offsetX by infiniteTransition.animateFloat(
        initialValue  = containerWidthPx.toFloat(),
        targetValue   = -textWidthPx.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(durationMillis = durationMs, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "marqueeOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerWidthPx = it.size.width }
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .clipToBounds()
    ) {
        Text(
            text       = message,
            color      = txtColor,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            softWrap   = false,
            onTextLayout = { result -> textWidthPx = result.size.width },
            modifier   = Modifier.offset(x = with(density) { offsetX.toDp() })
        )
    }
}

/**
 * Dialog cảnh báo DANGER — hiện khi vượt 400mg.
 * Chỉ hiện 1 lần mỗi session (remember flag isDismissed).
 */
@Composable
private fun CaffeineDangerDialog(caffeineMg: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Warning, null, tint = Color(0xFFEF5350), modifier = Modifier.size(36.dp)) },
        title = { Text("⚠️ Cảnh báo Caffeine!", fontWeight = FontWeight.Black, color = Color(0xFFEF5350)) },
        text = {
            Column {
                Text(
                    "Bạn đã nạp $caffeineMg mg Caffeine, vượt ngưỡng an toàn ${DrinkType.CAFFEINE_DANGER_MG}mg/ngày theo khái niệm y học.",
                    color = Color.White, fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "💧 Hãy chuyển sang uống nước lọc để bảo vệ giấc ngủ và sức khỏe tim mạch.",
                    color = Color(0xFFADAAAA), fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
            ) { Text("Đã hiểu", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF1A1A1A)
    )
}

// ====================================================================
// READY CONTENT — bố cục chính
// ====================================================================

@Composable
private fun WaterReadyContent(
    data: WaterScreenData,
    onLogWater: (Int, String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleReminder: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(DRINK_PRESETS[0]) }
    val cafLevel = caffeineLevel(data.totalCaffeineMg)

    // Tính nguồn caffeine đóng góp nhiều nhất hôm nay
    // WaterLogUiItem có drinkType + amountMl — tính lại caffeine theo DrinkType
    val dominantCaffeineSource by remember(data.todayLogs) {
        derivedStateOf {
            data.todayLogs
                .filter { !it.isDeleted }
                .groupBy { it.drinkType }
                .mapValues { (type, logs) ->
                    logs.sumOf { DrinkType.caffeineMg(type, it.amountMl) }
                }
                .maxByOrNull { it.value }
                ?.key ?: DrinkType.COFFEE
        }
    }

    // Hiện dialog DANGER 1 lần duy nhất mỗi session khi caffeine vượt 400mg
    var dangerDialogDismissed by remember { mutableStateOf(false) }
    val showDangerDialog = cafLevel == CaffeineLevel.DANGER && !dangerDialogDismissed

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Marquee Caffeine Ticker (text khác nhau theo nguồn: trà vs cà phê) ---
        CaffeineMarquee(
            caffeineMg     = data.totalCaffeineMg,
            dominantSource = dominantCaffeineSource
        )

        // Card tổng quan + cột nước
        WaterSummaryCard(data = data)

        // LazyRow chọn loại thức uống
        DrinkPresetRow(
            selected      = selectedPreset,
            caffeineMg    = data.totalCaffeineMg,
            onSelect      = { selectedPreset = it; showDialog = true }
        )

        // Danh sách lịch sử uống hôm nay (chỉ hiện 5 logs gần nhất)
        if (data.recentLogs.isNotEmpty()) {
            WaterLogList(logs = data.recentLogs, onDelete = onDelete)
        } else {
            WaterEmptyHistoryState()
        }

        // Biểu đồ tích lũy trong ngày
        WaterIntakeLineChartCard(
            chartData   = data.chartData,
            goalMl      = data.goalMl,
            consumedMl  = data.consumedMl
        )

        Spacer(Modifier.height(80.dp))
    }

    // Dialog nhập lượng
    if (showDialog) {
        AddWaterDialog(
            preset    = selectedPreset,
            isLoading = data.isLogging,
            onConfirm = { amount ->
                onLogWater(amount, selectedPreset.drinkType)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }

    // Dialog nguy hiểm (> 400mg Caffeine)
    if (showDangerDialog) {
        CaffeineDangerDialog(
            caffeineMg = data.totalCaffeineMg,
            onDismiss  = { dangerDialogDismissed = true }
        )
    }
}

// ====================================================================
// SUMMARY CARD — progress + hình người
// ====================================================================

@Composable
private fun WaterSummaryCard(data: WaterScreenData) {
    // UX Rule: Xanh dương mặc định, xanh lá khi đạt goal
    val goalReached = data.consumedMl >= data.goalMl && data.goalMl > 0
    val accentColor by animateColorAsState(
        if (goalReached) Color(0xFF81C784) else Color(0xFF64B5F6),
        tween(600), label = "waterGoalColor"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = data.progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "waterFill"
    )
    val humanPath = remember { Path() }
    val wavePath  = remember { Path() }
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase"
    )
    val fmt = { v: Int -> NumberFormat.getInstance(Locale("vi", "VN")).format(v) }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.WaterDrop, null, tint = accentColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (goalReached) "✓ Đã đạt mục tiêu!" else "Mục tiêu Nước",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = if (goalReached) accentColor else Color(0xFFADAAAA)
                )
            }
            Spacer(Modifier.height(28.dp))

            // Hình người
            Box(Modifier.size(140.dp, 280.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sx = size.width / 64f; val sy = size.height / 128f
                    if (humanPath.isEmpty) {
                        humanPath.apply {
                            moveTo(32f,4f); cubicTo(38.6f,4f,44f,9.4f,44f,16f); cubicTo(44f,22.6f,38.6f,28f,32f,28f)
                            cubicTo(25.4f,28f,20f,22.6f,20f,16f); cubicTo(20f,9.4f,25.4f,4f,32f,4f); close()
                            moveTo(32f,32f); cubicTo(20f,32f,13f,34f,12f,44f); lineTo(8f,76f)
                            cubicTo(8f,80f,12f,80f,14f,78f); lineTo(18f,50f); lineTo(20f,80f); lineTo(18f,120f)
                            cubicTo(18f,124f,26f,124f,26f,120f); lineTo(30f,84f)
                            cubicTo(31f,80f,33f,80f,34f,84f); lineTo(38f,120f)
                            cubicTo(38f,124f,46f,124f,46f,120f); lineTo(44f,80f); lineTo(46f,50f); lineTo(50f,78f)
                            cubicTo(52f,80f,56f,80f,56f,76f); lineTo(52f,44f)
                            cubicTo(51f,34f,44f,32f,32f,32f); close()
                        }
                    }
                    withTransform({ scale(sx, sy, pivot = androidx.compose.ui.geometry.Offset.Zero) }) {
                        drawPath(humanPath, Color(0xFF262626))
                        clipPath(humanPath) {
                            val fillH = 128f * (1f - minOf(animatedProgress, 1f))
                            wavePath.reset(); wavePath.moveTo(0f, fillH)
                            if (animatedProgress > 0f) {
                                for (x in 0..64 step 2) {
                                    wavePath.lineTo(x.toFloat(), fillH + sin((x/32f)*2*PI + wavePhase).toFloat()*2f)
                                }
                            } else { wavePath.lineTo(64f, fillH) }
                            wavePath.lineTo(64f,128f); wavePath.lineTo(0f,128f); wavePath.close()
                            drawPath(wavePath, accentColor)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("${fmt(data.consumedMl)} ml", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-1).sp)
            Text("/ ${fmt(data.goalMl)} ml (${(data.progress * 100).toInt()}%)", fontSize = 14.sp, color = Color(0xFFADAAAA))

            if (data.totalCaffeineMg > 0) {
                Spacer(Modifier.height(8.dp))
                Text("☕ Caffeine hôm nay: ${data.totalCaffeineMg}mg", fontSize = 12.sp, color = Color(0xFF8D6E63))
            }
        }
    }
}

// ====================================================================
// DRINK PRESET ROW — có màu sắc riêng từng loại
// ====================================================================

@Composable
private fun DrinkPresetRow(
    selected: DrinkPreset,
    caffeineMg: Int,
    onSelect: (DrinkPreset) -> Unit
) {
    val cafLevel = caffeineLevel(caffeineMg)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Các loại thức uống", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                // Hiện Hydration Index label của loại đang chọn
                val idx = (DrinkType.hydrationIndex(selected.drinkType) * 100).toInt()
                Text(
                    text = "HI: $idx%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        idx >= 90 -> Color(0xFF4CAF50)
                        idx >= 85 -> Color(0xFF8BC34A)
                        idx >= 80 -> Color(0xFFFFC107)
                        else      -> Color(0xFFFF7043)
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(DRINK_PRESETS) { preset ->
                    val meta = drinkMeta(preset.drinkType)
                    val isSelected = preset.drinkType == selected.drinkType

                    // Tính caffeine đóng góp ước tính của từng loại dựa trên amount mặc định nhỏ nhất
                    // → Cà phê 150ml = 90mg (0.60×150), Trà 150ml = 30mg (0.20×150)
                    // → 2 loại có "tốc độ tăng caffeine" KHÁC NHAU rõ ràng
                    val defaultAmount = preset.defaultAmounts.first()
                    val singleServingCaffeine = DrinkType.caffeineMg(preset.drinkType, defaultAmount)

                    // Border chỉ xuất hiện nếu:
                    // 1. Loại đó CÓ caffeine (singleServingCaffeine > 0)
                    // 2. Tổng caffeine hiện tại đã đủ để ly tiếp theo có thể gây vấn đề
                    //
                    // Ngưỡng viền CAM: tổng caffeine đã ≥ WARN (200mg)
                    // Ngưỡng viền ĐỎ:  tổng caffeine đã ≥ DANGER (400mg) HOẶC
                    //                  (tổng + 1 ly nữa) sẽ ≥ DANGER
                    val willExceedDanger = (caffeineMg + singleServingCaffeine) >= DrinkType.CAFFEINE_DANGER_MG
                    val borderColor = when {
                        singleServingCaffeine <= 0 -> Color.Transparent           // Không caffeine → không border
                        cafLevel == CaffeineLevel.DANGER -> Color(0xFFEF5350)     // Đã vượt ngưỡng → đỏ
                        willExceedDanger && cafLevel == CaffeineLevel.WARN -> Color(0xFFFF7043) // Ly tiếp theo sẽ vượt → cam đậm
                        cafLevel == CaffeineLevel.WARN -> Color(0xFFFFC107)       // Đang ở mức cảnh báo → vàng cam
                        else -> Color.Transparent
                    }
                    val borderWidth = if (borderColor != Color.Transparent) 2.dp else 0.dp

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(borderWidth, borderColor, CircleShape)
                                .background(if (isSelected) meta.color.copy(alpha = 0.25f) else Color(0xFF262626))
                                .clickable { onSelect(preset) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(meta.icon, null, tint = meta.color, modifier = Modifier.size(28.dp))
                            // Badge "!" chỉ khi DANGER hoặc ly tiếp theo sẽ vượt ngưỡng
                            if (singleServingCaffeine > 0 && (cafLevel == CaffeineLevel.DANGER || willExceedDanger)) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (cafLevel == CaffeineLevel.DANGER) Color(0xFFEF5350) else Color(0xFFFF7043)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("!", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(meta.label, fontSize = 11.sp, color = if (isSelected) meta.color else Color(0xFFADAAAA))
                    }
                }
            }
        }
    }
}

// ====================================================================
// WATER LOG LIST — lịch sử uống hôm nay
// ====================================================================

@Composable
private fun WaterLogList(
    logs: List<WaterLogUiItem>,
    onDelete: (String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Lịch sử hôm nay", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            logs.forEach { log ->
                val meta = drinkMeta(log.drinkType)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(meta.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center) {
                        Icon(meta.icon, null, tint = meta.color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(meta.label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Text(sdf.format(Date(log.timestamp)), fontSize = 11.sp, color = Color(0xFFADAAAA))
                    }
                    Text("+${log.amountMl} ml", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = meta.color)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { onDelete(log.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = Color(0xFF666666), modifier = Modifier.size(16.dp))
                    }
                }
                if (logs.indexOf(log) < logs.size - 1) {
                    HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun WaterEmptyHistoryState() {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.Opacity, null, tint = Color(0xFF333333), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Chưa có dữ liệu nước", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFADAAAA))
            Text("Hãy uống một ly nước để bắt đầu ngày mới!", fontSize = 12.sp, color = Color(0xFF666666), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ====================================================================
// ADD WATER DIALOG
// ====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWaterDialog(
    preset: DrinkPreset,
    isLoading: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val meta = drinkMeta(preset.drinkType)
    var customInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = { Icon(meta.icon, null, tint = meta.color, modifier = Modifier.size(32.dp)) },
        title = { Text("Thêm ${meta.label}", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column {
                Text("Phân lượng nhanh", color = Color(0xFFADAAAA), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    preset.defaultAmounts.take(4).forEach { amount ->
                        Button(
                            onClick = { onConfirm(amount) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = meta.color.copy(alpha = 0.2f), contentColor = meta.color),
                            enabled = !isLoading
                        ) { Text("$amount") }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it.filter(Char::isDigit) },
                    label = { Text("Tự nhập (ml)", color = Color(0xFFADAAAA)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF131313),
                        unfocusedContainerColor = Color(0xFF131313),
                        focusedBorderColor = meta.color,
                        focusedLabelColor = meta.color
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = customInput.toIntOrNull() ?: 0
                    if (v > 0) onConfirm(v)
                },
                enabled = !isLoading && (customInput.toIntOrNull() ?: 0) > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = meta.color)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = Color(0xFF0E0E0E), strokeWidth = 2.dp)
                else Text("Xác nhận", color = Color(0xFF0E0E0E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isLoading) onDismiss() }) {
                Text("Hủy", color = Color(0xFFADAAAA))
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFF1A1A1A)
    )
}

// ====================================================================
// WATER INTAKE LINE CHART — biểu đồ lượng nước tích lũy trong ngày
// ====================================================================

@Composable
private fun WaterIntakeLineChartCard(
    chartData: List<Float>,
    goalMl: Int,
    consumedMl: Int
) {
    if (consumedMl == 0 || chartData.isEmpty() || chartData.last() == 0f) {
        Card(
            Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.ShowChart, null, tint = Color(0xFF333333), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Chưa có biểu đồ", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFADAAAA))
                Text(
                    "Biểu đồ tích lũy sẽ xuất hiện sau khi bạn uống nước.",
                    fontSize = 12.sp, color = Color(0xFF666666),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp)
                )
            }
        }
        return
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tiến độ uống nước", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("${consumedMl} / ${goalMl} ml", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64B5F6))
            }
            Spacer(Modifier.height(24.dp))

            val points = chartData
            val maxY = maxOf(goalMl.toFloat(), points.maxOrNull() ?: 0f) * 1.1f // Padding 10% on top

            Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                val w = size.width
                val h = size.height
                val xSpacing = w / (points.size - 1)

                // 1. Draw Goal Line (Dashed)
                if (goalMl > 0) {
                    val goalY = h - (goalMl / maxY) * h
                    drawLine(
                        color = Color(0xFF333333),
                        start = androidx.compose.ui.geometry.Offset(0f, goalY),
                        end = androidx.compose.ui.geometry.Offset(w, goalY),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }

                // 2. Prepare Path
                val path = androidx.compose.ui.graphics.Path()
                points.forEachIndexed { i, value ->
                    val px = i * xSpacing
                    val py = h - (value / maxY) * h
                    if (i == 0) path.moveTo(px, py)
                    else path.lineTo(px, py)
                }

                // 3. Draw Fill Gradient
                val fillPath = androidx.compose.ui.graphics.Path().apply {
                    addPath(path)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFF64B5F6).copy(alpha = 0.3f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )

                // 4. Draw Stroke Line
                drawPath(
                    path = path,
                    color = Color(0xFF64B5F6),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // 5. Draw Data Points (Dots)
                points.forEachIndexed { i, value ->
                    val px = i * xSpacing
                    val py = h - (value / maxY) * h
                    drawCircle(Color(0xFF1A1A1A), radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(px, py))
                    drawCircle(Color(0xFF64B5F6), radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(px, py))
                }
            }

            // 6. Draw X-Axis Labels (00, 02, 04... 24)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "02", "04", "06", "08", "10", "12", "14", "16", "18", "20", "22", "24").forEach { label ->
                    Text(label, fontSize = 9.sp, color = Color(0xFF666666))
                }
            }
        }
    }
}
