package com.example.finfit.health.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.finfit.health.repository.ChartPoint
import com.example.finfit.health.repository.NutritionPeriod
import com.example.finfit.health.repository.NutritionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FoodScannerScreen(
    userEmail: String,
    onBack: () -> Unit,
    onNavigateToCamera: (String) -> Unit = {},
    onHome: () -> Unit = onBack,
    viewModel: NutritionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMealNameDialog by remember { mutableStateOf(false) }
    var mealNameInput by remember { mutableStateOf("") }
    
    // Staggered Entry Animation State
    var headerVisible by remember { mutableStateOf(false) }
    var summaryVisible by remember { mutableStateOf(false) }
    var chartVisible by remember { mutableStateOf(false) }
    var mealsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        headerVisible = true
        delay(100)
        summaryVisible = true
        delay(200)
        chartVisible = true
        delay(150)
        mealsVisible = true
    }

    if (showMealNameDialog) {
        AlertDialog(
            onDismissRequest = { showMealNameDialog = false },
            title = { Text(text = "Tạo bữa ăn mới", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = mealNameInput,
                    onValueChange = { mealNameInput = it },
                    label = { Text("Tên bữa ăn (VD: Ăn vặt)", color = Color(0xFFadaaaa)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF64b5f6),
                        unfocusedBorderColor = Color(0xFF383838),
                        focusedContainerColor = Color(0xFF141414),
                        unfocusedContainerColor = Color(0xFF141414)
                    ),
                    singleLine = true
                )
            },
            containerColor = Color(0xFF1a1a1a),
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalName = if (mealNameInput.isNotBlank()) mealNameInput else "Snack"
                        showMealNameDialog = false
                        mealNameInput = ""
                        onNavigateToCamera(finalName)
                    }
                ) {
                    Text("Tiếp tục", color = Color(0xFF64b5f6), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMealNameDialog = false }) {
                    Text("Hủy", color = Color(0xFFadaaaa))
                }
            }
        )
    }

    // Meal Detail Popup
    uiState.selectedMeal?.let { meal ->
        MealDetailPopup(
            meal = meal,
            items = uiState.selectedMealItems,
            onDismiss = { viewModel.dismissMeal() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
    ) {
        // 1. Header with Fade Animation
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { -40 })
        ) {
            HealthHeaderSection(
                title = "Dinh dưỡng",
                userEmail = userEmail,
                showBackButton = true,
                onBackClick = onBack,
                onHomeClick = onHome,
                actionIcon = Icons.Rounded.DocumentScanner,
                onActionClick = { showMealNameDialog = true }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 2. Daily Energy Circular Progress & Macro Cards
            item {
                AnimatedVisibility(
                    visible = summaryVisible,
                    enter = fadeIn(tween(800)) + slideInVertically(initialOffsetY = { 60 })
                ) {
                    PremiumNutritionSummary(uiState.todaySummary)
                }
            }

            // 3. Nutrition Chart with Period Switcher
            item {
                AnimatedVisibility(
                    visible = chartVisible,
                    enter = fadeIn(tween(1000)) + slideInVertically(initialOffsetY = { 100 })
                ) {
                    PremiumAnimatedChart(
                        period = uiState.period,
                        data = uiState.chartData,
                        goal = uiState.todaySummary.calorieGoal,
                        onPeriodChange = { viewModel.setPeriod(it) }
                    )
                }
            }

            // 4. Recent Meals
            item {
                AnimatedVisibility(
                    visible = mealsVisible,
                    enter = fadeIn(tween(800, delayMillis = 200))
                ) {
                    RecentMealsList(
                        meals = uiState.meals,
                        onMealClick = { viewModel.selectMeal(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumNutritionSummary(summary: com.example.finfit.health.model.HealthUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Năng lượng hôm nay",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // Main Circular Gauge
                val caloriesAnim by animateIntAsState(
                    targetValue = summary.caloriesIn,
                    animationSpec = tween(1500, easing = FastOutSlowInEasing),
                    label = "calories"
                )
                
                NutritionCircularGauge(
                    current = summary.caloriesIn.toFloat(),
                    goal = summary.calorieGoal.toFloat(),
                    color = Color(0xFF64b5f6)
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%,d", caloriesAnim),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "kcal / ${String.format("%,d", summary.calorieGoal)}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Macro Progress Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroVisualCard(
                    label = "Carbs",
                    current = summary.carbs,
                    goal = summary.carbsGoal,
                    color = Color(0xFF64b5f6)
                )
                MacroVisualCard(
                    label = "Protein",
                    current = summary.protein,
                    goal = summary.proteinGoal,
                    color = Color(0xFF81c784)
                )
                MacroVisualCard(
                    label = "Fat",
                    current = summary.fat,
                    goal = summary.fatGoal,
                    color = Color(0xFFf06292)
                )
            }
        }
    }
}

@Composable
fun NutritionCircularGauge(current: Float, goal: Float, color: Color) {
    val progress = if (goal > 0) (current / goal).coerceIn(0f, 1.2f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        val strokeWidth = 20.dp.toPx()
        val innerSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val offset = Offset(strokeWidth / 2, strokeWidth / 2)

        // Background track
        drawArc(
            color = Color.White.copy(alpha = 0.05f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = offset,
            size = innerSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Gradient Progress
        val gradient = Brush.sweepGradient(
            0.0f to color.copy(alpha = 0.3f),
            animatedProgress.coerceIn(0f, 1f) to color,
            center = center
        )

        drawArc(
            brush = Brush.linearGradient(listOf(color.copy(alpha = 0.7f), color)),
            startAngle = 135f,
            sweepAngle = 270f * animatedProgress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = offset,
            size = innerSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Overflow indicator (if over goal)
        if (animatedProgress > 1f) {
            drawArc(
                color = color.copy(alpha = 0.3f),
                startAngle = 135f,
                sweepAngle = 270f * (animatedProgress - 1f).coerceIn(0f, 1f),
                useCenter = false,
                topLeft = offset,
                size = innerSize,
                style = Stroke(width = strokeWidth * 0.4f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun MacroVisualCard(label: String, current: Int, goal: Int, color: Color) {
    val progress = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "macro_progress"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Mini Circular Progress
            Canvas(modifier = Modifier.size(48.dp)) {
                val stroke = 5.dp.toPx()
                drawCircle(color = Color.White.copy(alpha = 0.05f), style = Stroke(width = stroke))
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = "${current}g", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PremiumAnimatedChart(
    period: NutritionPeriod,
    data: List<ChartPoint>,
    goal: Int,
    onPeriodChange: (NutritionPeriod) -> Unit
) {
    var selectedPoint by remember { mutableStateOf<ChartPoint?>(null) }
    var selectedDotOffset by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    // px per hour slot in Day mode — 48dp each gives a scrollable 24h canvas
    val hourSlotDp = 48.dp
    val totalChartWidthDp = hourSlotDp * 24  // 1152.dp

    val scrollState = rememberScrollState()

    // Auto-scroll to current hour on first composition (Day mode)
    LaunchedEffect(period) {
        if (period == NutritionPeriod.Day && data.isNotEmpty()) {
            // Scroll so current hour is roughly centered
            val targetPx = with(androidx.compose.ui.platform.LocalDensity) { 0 } // placeholder
            // Use rough estimate: 48dp * currentHour, center offset handled below
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(NutritionPeriod.Day, NutritionPeriod.Week).forEach { mode ->
                    val isSelected = period == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF64b5f6) else Color.Transparent)
                            .clickable { onPeriodChange(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                NutritionPeriod.Day -> "Ngày"
                                NutritionPeriod.Week -> "Tuần"
                            },
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                if (data.isEmpty()) {
                    NutritionEmptyState()
                } else {
                    AnimatedContent(
                        targetState = period,
                        transitionSpec = {
                            (fadeIn(tween(600)) + slideInHorizontally { it / 2 }).togetherWith(
                                fadeOut(tween(400)) + slideOutHorizontally { -it / 2 }
                            )
                        },
                        label = "chart_mode_transition"
                    ) { targetPeriod ->
                        if (targetPeriod == NutritionPeriod.Day) {
                            // ---- 24H SCROLLABLE DAY CHART ----
                            val density = LocalDensity.current
                            val hourSlotPx = with(density) { hourSlotDp.toPx() }
                            val totalWidthPx = hourSlotPx * 24

                            // Auto-scroll to currentHour (center it)
                            val viewportWidthPx = remember { mutableStateOf(0f) }
                            LaunchedEffect(period, viewportWidthPx.value) {
                                if (viewportWidthPx.value > 0) {
                                    val targetScroll = ((currentHour * hourSlotPx) - viewportWidthPx.value / 2)
                                        .toInt().coerceAtLeast(0)
                                    scrollState.scrollTo(targetScroll)
                                }
                            }

                            Column {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .horizontalScroll(scrollState)
                                        .onSizeChanged { viewportWidthPx.value = it.width.toFloat() }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(totalChartWidthDp)
                                            .fillMaxHeight()
                                            .pointerInput(data) {
                                                detectTapGestures(onTap = { offset ->
                                                    val hourIndex = (offset.x / hourSlotPx)
                                                        .toInt().coerceIn(0, 23)
                                                    val pt = data.getOrNull(hourIndex)
                                                    if (pt != null) {
                                                        selectedPoint = pt
                                                        val chartHeight = size.height.toFloat()
                                                        val rawMax = maxOf(
                                                            data.map { it.value }.maxOrNull() ?: 0f,
                                                            goal.toFloat(), 1f
                                                        ) * 1.2f
                                                        val y = chartHeight - (pt.value / rawMax * chartHeight)
                                                        selectedDotOffset = Offset(offset.x, y)
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                })
                                            }
                                    ) {
                                        InteractiveTrendChart(
                                            data = data,
                                            goal = goal.toFloat(),
                                            isDaily = true,
                                            selectedHour = selectedPoint?.time?.substringBefore(":")?.toIntOrNull(),
                                            selectedDotOffset = selectedDotOffset
                                        )
                                    }
                                }

                                // X-Axis Labels row (synced scroll)
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(scrollState)
                                ) {
                                    Row(
                                        modifier = Modifier.width(totalChartWidthDp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        data.forEachIndexed { idx, point ->
                                            Box(
                                                modifier = Modifier.width(hourSlotDp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (point.label.isNotEmpty()) {
                                                    Text(
                                                        text = point.label,
                                                        color = if (idx == currentHour)
                                                            Color(0xFF64b5f6)
                                                        else
                                                            Color.White.copy(alpha = 0.35f),
                                                        fontSize = 9.sp,
                                                        fontWeight = if (idx == currentHour) FontWeight.Bold else FontWeight.Normal,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // ---- WEEKLY CHART (unchanged layout) ----
                            Column {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .pointerInput(data) {
                                                detectTapGestures(onTap = { offset ->
                                                    if (data.size >= 2) {
                                                        val sidePadding = 24.dp.toPx()
                                                        val usableWidth = size.width - (sidePadding * 2)
                                                        val step = usableWidth / (data.size - 1)
                                                        if (step > 0) {
                                                            val index = ((offset.x - sidePadding) / step)
                                                                .toInt().coerceIn(0, data.size - 1)
                                                            val pt = data.getOrNull(index)
                                                            if (pt != null) {
                                                                selectedPoint = pt
                                                                selectedDotOffset = offset
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            }
                                                        }
                                                    }
                                                })
                                            }
                                    ) {
                                        InteractiveTrendChart(
                                            data = data,
                                            goal = goal.toFloat(),
                                            isDaily = false,
                                            selectedHour = null,
                                            selectedDotOffset = Offset.Zero
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    data.forEach { point ->
                                        if (point.label.isNotEmpty()) {
                                            Text(
                                                text = point.label,
                                                color = if (point.isHighlighted) Color(0xFF64b5f6)
                                                else Color.White.copy(alpha = 0.3f),
                                                fontSize = 10.sp,
                                                fontWeight = if (point.isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Single floating tooltip
                selectedPoint?.let { point ->
                    ChartTooltip(
                        point = point,
                        offset = selectedDotOffset,
                        onDismiss = { selectedPoint = null }
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveTrendChart(
    data: List<ChartPoint>,
    goal: Float,
    isDaily: Boolean = false,
    selectedHour: Int? = null,
    selectedDotOffset: Offset = Offset.Zero
) {
    val rawMax = maxOf(data.map { it.value }.maxOrNull() ?: 0f, goal, 1f)
    val maxVal = rawMax * 1.2f

    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }

    val path = remember(data, maxVal) { Path() }
    val fillPath = remember(data, maxVal) { Path() }

    // Pulsing animation for the selected dot
    val pulseAnim = rememberInfiniteTransition(label = "dot_pulse")
    val pulseRadius by pulseAnim.animateFloat(
        initialValue = 6f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val goalY = height - (goal / maxVal * height)

            // For Day mode: each hour gets an equal slot width
            val step = if (isDaily) {
                width / 24f
            } else {
                val sidePadding = 24.dp.toPx()
                (width - sidePadding * 2) / (data.size - 1).coerceAtLeast(1)
            }
            val xOffset = if (isDaily) 0f else 24.dp.toPx()

            if (data.isNotEmpty()) {
                path.reset()
                fillPath.reset()

                data.forEachIndexed { i, point ->
                    val x = xOffset + i * step + if (isDaily) step / 2f else 0f
                    val y = height - (point.value / maxVal * height)

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = xOffset + (i - 1) * step + if (isDaily) step / 2f else 0f
                        val prevY = height - (data[i - 1].value / maxVal * height)
                        path.cubicTo(
                            (prevX + x) / 2f, prevY,
                            (prevX + x) / 2f, y,
                            x, y
                        )
                        fillPath.cubicTo(
                            (prevX + x) / 2f, prevY,
                            (prevX + x) / 2f, y,
                            x, y
                        )
                    }
                    if (i == data.size - 1) {
                        fillPath.lineTo(x, height)
                        fillPath.close()
                    }
                }
            }

            // --- Draw layers ---
            clipRect(right = width * drawProgress.value) {
                drawPath(
                    path = path,
                    color = Color(0xFF64b5f6),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF64b5f6).copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                if (goal > 0) {
                    clipRect(top = 0f, bottom = goalY) {
                        drawPath(
                            path = path,
                            color = Color(0xFFffb74d),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFffb74d).copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                    }
                }
            }

            // --- Single dot: only the tapped/selected point ---
            if (selectedHour != null && selectedDotOffset != Offset.Zero) {
                val dotX = selectedDotOffset.x
                val dotY = selectedDotOffset.y
                val pt = data.getOrNull(selectedHour)
                val isExceeded = (pt?.value ?: 0f) > goal && goal > 0
                val dotColor = if (isExceeded) Color(0xFFffb74d) else Color(0xFF64b5f6)

                // Outer pulsing ring
                drawCircle(color = dotColor.copy(alpha = 0.25f), radius = pulseRadius.dp.toPx(), center = Offset(dotX, dotY))
                // Inner solid dot
                drawCircle(color = dotColor, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
                // White center
                drawCircle(color = Color.White, radius = 2.5f.dp.toPx(), center = Offset(dotX, dotY))
            } else if (!isDaily) {
                // Weekly mode: show dot only for highlighted (today) point
                data.forEachIndexed { i, point ->
                    if (point.isHighlighted && point.value > 0) {
                        val sidePadding = 24.dp.toPx()
                        val usableWidth = size.width - sidePadding * 2
                        val s = usableWidth / (data.size - 1).coerceAtLeast(1)
                        val x = sidePadding + i * s
                        val y = size.height - (point.value / maxVal * size.height)
                        drawCircle(color = Color(0xFF64b5f6).copy(alpha = 0.3f), radius = 8.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = Color(0xFF64b5f6), radius = 4.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
        }

        GoalLine(goal = goal, maxVal = maxVal)
    }
}

@Composable
fun GoalLine(goal: Float, maxVal: Float) {
    if (goal <= 0) return
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height - (goal / maxVal * size.height)
            drawLine(
                color = Color(0xFFffb74d).copy(alpha = 0.4f), // Dashed Orange/Amber
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        }
        
        // Goal Label
        val yPos = remember(goal, maxVal) { (1f - (goal / maxVal)).coerceIn(0f, 0.9f) }
        Box(
            modifier = Modifier
                .fillMaxHeight(yPos)
                .padding(end = 8.dp)
                .align(Alignment.BottomEnd)
        ) {
            Text(
                text = "TARGET: ${goal.toInt()} kcal",
                color = Color(0xFFffb74d).copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = (-6).dp)
            )
        }
    }
}

@Composable
fun ChartTooltip(point: ChartPoint, offset: Offset, onDismiss: () -> Unit) {
    LaunchedEffect(point) {
        delay(3000)
        onDismiss()
    }
    
    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.toInt() - 75, offset.y.toInt() - 110) }
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .background(Color(0xFF202020), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .animateContentSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (point.metadata != null) {
                Text(
                    text = point.metadata,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = point.time ?: "",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            } else {
                Text(point.label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${point.value.toInt()} kcal",
                color = if (point.value > 0) Color(0xFF64b5f6) else Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun NutritionEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Restaurant,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Chưa có dữ liệu dinh dưỡng",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            "Hãy quét món ăn đầu tiên của bạn!",
            color = Color(0xFF64b5f6).copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun MealDetailPopup(
    meal: com.example.finfit.health.model.FoodMealEntity,
    items: List<com.example.finfit.health.model.MealItemEntity>,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            color = Color(0xFF0e0e0e)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        text = "Chi tiết bữa ăn",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    // Main Image
                    if (meal.previewImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = meal.previewImageUrl,
                            contentDescription = meal.mealName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Text(
                        text = meal.mealName,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    Text(
                        text = SimpleDateFormat("HH:mm, EEEE, dd/MM", Locale.getDefault()).format(Date(meal.createdAt)),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Macro Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MealMacroIndicator("Calories", meal.totalCalories, "kcal", Color(0xFF64b5f6))
                        MealMacroIndicator("Protein", meal.totalProtein, "g", Color(0xFF81c784))
                        MealMacroIndicator("Carbs", meal.totalCarbs, "g", Color(0xFFffb74d))
                        MealMacroIndicator("Fat", meal.totalFat, "g", Color(0xFFf06292))
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Items List
                    Text("Thành phần", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF64b5f6))
                        }
                    } else {
                        items.forEach { item ->
                            DetectedItemCard(item)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun MealMacroIndicator(label: String, value: Int, unit: String, color: Color) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "$value", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = unit, color = color, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp, start = 2.dp))
        }
    }
}

@Composable
fun DetectedItemCard(item: com.example.finfit.health.model.MealItemEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.itemName,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.itemName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${item.calories} kcal • ${item.protein}g P",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF81c784).copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${(item.confidence * 100).toInt()}%",
                    color = Color(0xFF81c784),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RecentMealsList(
    meals: List<com.example.finfit.health.model.FoodMealEntity>,
    onMealClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bữa ăn hôm nay", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = {}) {
                Text("Xem tất cả", color = Color(0xFF64b5f6), fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (meals.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
            ) {
                Box(modifier = Modifier.padding(40.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Chưa có bữa ăn nào hôm nay", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                }
            }
        } else {
            meals.forEach { meal ->
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(meal.createdAt))
                MealListItem(
                    name = meal.mealName,
                    time = timeStr,
                    kcal = meal.totalCalories.toString(),
                    imageUrl = meal.previewImageUrl,
                    onClick = { onMealClick(meal.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun MealListItem(
    name: String,
    time: String,
    kcal: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Restaurant, contentDescription = name, tint = Color.White.copy(alpha = 0.2f))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(time, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(kcal, color = Color(0xFF64b5f6), fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("kcal", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
            }
        }
    }
}

