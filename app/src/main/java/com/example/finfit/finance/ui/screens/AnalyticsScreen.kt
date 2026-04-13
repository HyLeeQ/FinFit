package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.AccentGreen
import com.example.finfit.ui.theme.PrimaryBlue
import java.util.*
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

enum class AnalyticsPeriod { DAY, WEEK, MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    transactions: List<FinanceTransaction>,
    onBack: () -> Unit
) {
    var period by remember { mutableStateOf(AnalyticsPeriod.WEEK) }
    var weekOffset by remember { mutableIntStateOf(0) }
    
    var showTypeDetail by remember { mutableStateOf<TransactionType?>(null) }
    var selectedSliceIndex by remember { mutableIntStateOf(-1) }

    // Lọc giao dịch theo kỳ hạn được chọn
    val filteredTransactions = remember(transactions, period, weekOffset) {
        val now = Calendar.getInstance()
        if (period == AnalyticsPeriod.WEEK) now.add(Calendar.WEEK_OF_YEAR, weekOffset)
        
        transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { time = tx.timestamp.toDate() }
            when (period) {
                AnalyticsPeriod.DAY -> isSameDay(txCal, now)
                AnalyticsPeriod.WEEK -> isSameWeek(txCal, now)
                AnalyticsPeriod.MONTH -> isSameMonth(txCal, now)
                AnalyticsPeriod.YEAR -> isSameYear(txCal, now)
            }
        }
    }

    val expenseTransactions = remember(filteredTransactions) { filteredTransactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT } }
    val incomeTransactions = remember(filteredTransactions) { filteredTransactions.filter { it.type == TransactionType.INCOME } }
    
    val totalExpense = expenseTransactions.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
    val totalIncome = incomeTransactions.sumOf { it.amount }
    
    val categoryStats = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> if (tx.isGroupPrepayment) tx.personalAmount else tx.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val dailyTrend = remember(transactions, weekOffset) { calculateDailyTrend(transactions, weekOffset) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (showTypeDetail != null) {
                            if (showTypeDetail == TransactionType.INCOME) "Chi tiết thu nhập" else "Chi tiết chi tiêu"
                        } else "Phân tích tài chính", 
                        fontWeight = FontWeight.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (showTypeDetail != null) showTypeDetail = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showTypeDetail == null) {
                // Period Selector
                PeriodSelectorMenu(period) { period = it; weekOffset = 0 }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(Modifier.height(8.dp))
                    
                    // Summary Cards
                    IncomeExpenseSummary(
                        income = totalIncome, 
                        expense = totalExpense,
                        onIncomeClick = { showTypeDetail = TransactionType.INCOME },
                        onExpenseClick = { showTypeDetail = TransactionType.EXPENSE }
                    )

                    // Spending Breakdown (Donut Chart)
                    if (categoryStats.isNotEmpty()) {
                        SpendingBreakdownCard(
                            stats = categoryStats, 
                            total = totalExpense, 
                            selectedIndex = selectedSliceIndex,
                            onSliceClick = { selectedSliceIndex = it }
                        )
                    } else {
                        NoDataView("Không có dữ liệu chi tiêu trong kỳ này")
                    }

                    // Spending Trend (Bar Chart)
                    DailyTrendCard(dailyTrend, weekOffset, onOffsetChange = { weekOffset += it })
                    
                    Spacer(Modifier.height(100.dp))
                }
            } else {
                // Hiển thị danh sách chi tiết (List View)
                val detailList = if (showTypeDetail == TransactionType.INCOME) incomeTransactions else expenseTransactions
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    item { 
                        Text(
                            "Danh sách giao dịch", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    items(detailList) { tx ->
                        TransactionDetailRow(tx)
                    }
                    if (detailList.isEmpty()) {
                        item { NoDataView("Chưa có giao dịch nào") }
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun PeriodSelectorMenu(selected: AnalyticsPeriod, onSelect: (AnalyticsPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        AnalyticsPeriod.values().forEach { p ->
            val isSelected = selected == p
            val label = when(p) {
                AnalyticsPeriod.DAY -> "Ngày"
                AnalyticsPeriod.WEEK -> "Tuần"
                AnalyticsPeriod.MONTH -> "Tháng"
                AnalyticsPeriod.YEAR -> "Năm"
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) PrimaryBlue else Color.Transparent)
                    .clickable { onSelect(p) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IncomeExpenseSummary(income: Double, expense: Double, onIncomeClick: () -> Unit, onExpenseClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SummaryMiniCard(
            modifier = Modifier.weight(1f).clickable { onIncomeClick() },
            label = "THU NHẬP",
            amount = income,
            color = AccentGreen,
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        SummaryMiniCard(
            modifier = Modifier.weight(1f).clickable { onExpenseClick() },
            label = "CHI TIÊU",
            amount = expense,
            color = Color(0xFFEF4444),
            icon = Icons.AutoMirrored.Filled.TrendingDown
        )
    }
}

@Composable
fun SummaryMiniCard(modifier: Modifier, label: String, amount: Double, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
                }
                Spacer(Modifier.width(10.dp))
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
            }
            Spacer(Modifier.height(16.dp))
            AnimatedAmountText(amount, false, MaterialTheme.colorScheme.onBackground, 18.sp, FontWeight.Black)
        }
    }
}

@Composable
fun SpendingBreakdownCard(stats: List<Pair<String, Double>>, total: Double, selectedIndex: Int, onSliceClick: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Phân bổ chi tiêu", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart
                Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    SelectableDonutChart(
                        stats = stats,
                        total = total,
                        selectedIndex = selectedIndex,
                        onSliceClick = onSliceClick,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Center Content
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selectedIndex != -1 && selectedIndex < stats.size) {
                            val item = stats[selectedIndex]
                            Text(item.first, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("${((item.second / total) * 100).toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Black, color = getChartColor(selectedIndex))
                        } else {
                            Text("Tổng", fontSize = 12.sp, color = Color.Gray)
                            Text("${(total/1_000).toInt()}k", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                
                Spacer(Modifier.width(24.dp))
                
                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    stats.take(5).forEachIndexed { index, pair ->
                        LegendItem(pair.first, pair.second/total, getChartColor(index), selectedIndex == index) { onSliceClick(index) }
                    }
                    if (stats.size > 5) {
                        Text("và ${stats.size - 5} nhóm khác", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SelectableDonutChart(
    stats: List<Pair<String, Double>>, 
    total: Double, 
    selectedIndex: Int,
    onSliceClick: (Int) -> Unit,
    modifier: Modifier
) {
    Canvas(modifier = modifier.pointerInput(stats, total) {
        detectTapGestures { offset ->
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val x = offset.x - centerX
            val y = offset.y - centerY
            
            val radius = sqrt(x.pow(2) + y.pow(2))
            val innerRadius = 60.dp.toPx()
            val outerRadius = 100.dp.toPx()
            
            if (radius in innerRadius..outerRadius) {
                var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                if (angle < 0) angle += 360f
                
                // Canvas starts angle at -90 degrees
                var currentAngle = 270f 
                var clickedIndex = -1
                
                stats.forEachIndexed { index, pair ->
                    val sweep = (pair.second / total).toFloat() * 360f
                    val normalizedStart = currentAngle % 360f
                    val normalizedEnd = (currentAngle + sweep) % 360f
                    
                    val isWithin = if (normalizedStart < normalizedEnd) {
                        angle in normalizedStart..normalizedEnd
                    } else {
                        angle >= normalizedStart || angle <= normalizedEnd
                    }
                    
                    if (isWithin) {
                        clickedIndex = index
                    }
                    currentAngle += sweep
                }
                onSliceClick(clickedIndex)
            } else {
                onSliceClick(-1)
            }
        }
    }) {
        var startAngle = -90f
        stats.forEachIndexed { index, pair ->
            val sweepAngle = (pair.second / total).toFloat() * 360f
            val isSelected = selectedIndex == index
            
            drawArc(
                color = getChartColor(index),
                startAngle = startAngle + 2f, // Small gap
                sweepAngle = sweepAngle - 4f,
                useCenter = false,
                style = Stroke(width = if (isSelected) 48f else 36f, cap = StrokeCap.Round),
                alpha = if (selectedIndex == -1 || isSelected) 1f else 0.3f
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun LegendItem(label: String, percentage: Double, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(4.dp)
    ) {
        Box(Modifier.size(if (isSelected) 12.dp else 8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(
            label, 
            fontSize = 12.sp, 
            modifier = Modifier.weight(1f), 
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
        )
        Text("${(percentage * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.8f))
    }
}

@Composable
fun DailyTrendCard(trend: List<Pair<String, Double>>, currentOffset: Int, onOffsetChange: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Xu hướng chi tiêu", fontWeight = FontWeight.Black, fontSize = 18.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onOffsetChange(-1) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = PrimaryBlue)
                    }
                    Text(
                        if (currentOffset == 0) "Tuần này" else "Cách đây ${-currentOffset} tuần", 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { if (currentOffset < 0) onOffsetChange(1) }, modifier = Modifier.size(24.dp), enabled = currentOffset < 0) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = if (currentOffset < 0) PrimaryBlue else Color.LightGray)
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            // Tìm giá trị max để scale, nhưng đảm bảo không quá nhỏ
            val maxVal = (trend.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
            
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEachIndexed { index, (day, amount) ->
                    val hRatio = (amount / maxVal).toFloat().coerceIn(0.05f, 1.0f)
                    
                    val animatedHeight by animateFloatAsState(
                        targetValue = hRatio,
                        animationSpec = tween(1000, delayMillis = index * 50, easing = FastOutSlowInEasing),
                        label = "BarHeight"
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.height(140.dp).width(20.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(animatedHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(PrimaryBlue, PrimaryBlue.copy(alpha = 0.4f))
                                        )
                                    )
                            )
                            
                            if (amount > 0) {
                                Text(
                                    if (amount >= 1_000_000) "${(amount/1_000_000).toInt()}M" else "${(amount/1_000).toInt()}k", 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = PrimaryBlue,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-16).dp),
                                    maxLines = 1
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text(day, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (day == "CN") Color.Red.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionDetailRow(tx: FinanceTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(tx.category.take(1).uppercase(), fontWeight = FontWeight.Black, color = PrimaryBlue)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.category, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (tx.note.isNotBlank()) Text(tx.note, fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                "${if (tx.type == TransactionType.INCOME) "+" else "-"} ${formatCurrency(if (tx.isGroupPrepayment) tx.personalAmount else tx.amount)}",
                fontWeight = FontWeight.Black,
                color = if (tx.type == TransactionType.INCOME) AccentGreen else Color(0xFFEF4444)
            )
            if (tx.isGroupPrepayment) {
                Spacer(Modifier.width(4.dp))
                Text("(Hộ: ${formatCurrency(tx.groupAmount)})", fontSize = 10.sp, color = PrimaryBlue)
            }
        }
    }
}

@Composable
fun NoDataView(text: String) {
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

/** Utility Helpers */

fun calculateDailyTrend(transactions: List<FinanceTransaction>, weekOffset: Int): List<Pair<String, Double>> {
    val trend = mutableListOf<Pair<String, Double>>()
    val now = Calendar.getInstance()
    now.add(Calendar.WEEK_OF_YEAR, weekOffset)
    
    // Tìm ngày bắt đầu tuần (T2)
    val startOfWeek = now.clone() as Calendar
    startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    
    for (i in 0..6) {
        val d = startOfWeek.clone() as Calendar
        d.add(Calendar.DAY_OF_YEAR, i)
        
        val dayLabel = when(d.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "T2"
            Calendar.TUESDAY -> "T3"
            Calendar.WEDNESDAY -> "T4"
            Calendar.THURSDAY -> "T5"
            Calendar.FRIDAY -> "T6"
            Calendar.SATURDAY -> "T7"
            else -> "CN"
        }
        
        val total = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { time = tx.timestamp.toDate() }
            (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT) && isSameDay(txCal, d)
        }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
        
        trend.add(dayLabel to total)
    }
    return trend
}

fun isSameDay(c1: Calendar, c2: Calendar) = 
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

fun isSameWeek(c1: Calendar, c2: Calendar) = 
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR)

fun isSameMonth(c1: Calendar, c2: Calendar) = 
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)

fun isSameYear(c1: Calendar, c2: Calendar) = 
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)

fun getChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), 
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFF06B6D4), Color(0xFFF97316)
    )
    return colors[index % colors.size]
}
