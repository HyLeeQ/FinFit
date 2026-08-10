package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*
import com.example.finfit.finance.ui.logic.*
import com.example.finfit.finance.util.*
import com.example.finfit.core.ui.FinFitTopAppBar

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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

enum class AnalyticsViewTab(val label: String, val iconEmoji: String) {
    OVERVIEW("Tổng quan", "📊"),
    COMPARISON("So sánh kỳ", "⚖️"),
    ADVANCED("Phân tích sâu", "⚡")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    transactions: List<FinanceTransaction>,
    wallet: AppUserWallet? = null,
    budgets: List<FinanceBudget> = emptyList(),
    goals: List<SavingsGoal> = emptyList(),
    debtLoans: List<DebtLoan> = emptyList(),
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AnalyticsViewTab.OVERVIEW) }
    var period by remember { mutableStateOf(AnalyticsPeriod.MONTH) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var comparisonMode by remember { mutableStateOf(ComparisonMode.MONTH_OVER_MONTH) }
    
    var showTypeDetail by remember { mutableStateOf<TransactionType?>(null) }
    var selectedSliceIndex by remember { mutableIntStateOf(-1) }
    var showHealthDialog by remember { mutableStateOf(false) }

    // Tính điểm sức khỏe tài chính (0-100)
    val healthResult = remember(wallet, transactions, budgets, goals, debtLoans) {
        FinancialHealthCalculator.calculate(wallet, transactions, budgets, goals, debtLoans)
    }

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
    val dayOfWeekAnalysis = remember(transactions) { analyzeDayOfWeekSpending(transactions) }
    val periodComparison = remember(transactions, comparisonMode) { calculatePeriodComparison(transactions, comparisonMode) }
    val anomalyCategories = remember(transactions) { detectAnomalyCategories(transactions) }
    val netCashFlowHistory = remember(transactions) { calculateNetCashFlowHistory(transactions) }

    Scaffold(
        topBar = {
            FinFitTopAppBar(
                title = if (showTypeDetail != null) {
                    if (showTypeDetail == TransactionType.INCOME) "Chi tiết thu nhập" else "Chi tiết chi tiêu"
                } else "Phân tích tài chính",
                onBack = { if (showTypeDetail != null) showTypeDetail = null else onBack() },
                containerColor = MaterialTheme.colorScheme.background
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
                // Main View Tabs (Tổng quan / So sánh kỳ / Phân tích sâu)
                AnalyticsTabHeader(selectedTab) { selectedTab = it }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    // Financial Health Card Header (Always visible & interactive)
                    FinancialHealthOverviewCard(healthResult) { showHealthDialog = true }

                    when (selectedTab) {
                        AnalyticsViewTab.OVERVIEW -> {
                            // Period Selector
                            PeriodSelectorMenu(period) { period = it; weekOffset = 0 }

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
                        }

                        AnalyticsViewTab.COMPARISON -> {
                            // Mode Switcher (Tháng này vs Tháng trước / Quý / Tuần)
                            ComparisonModeSelector(comparisonMode) { comparisonMode = it }

                            // Period Comparison View
                            PeriodComparisonCard(periodComparison)
                        }

                        AnalyticsViewTab.ADVANCED -> {
                            // 1. Day of week peak spending insight
                            DayOfWeekAnalysisCard(dayOfWeekAnalysis)

                            // 2. Top 5 Anomaly Categories (Biến động bất thường)
                            AnomalyCategoriesCard(anomalyCategories)

                            // 3. Net Cash Flow Trend (Dòng tiền ròng)
                            NetCashFlowTrendCard(netCashFlowHistory)
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            } else {
                // Hiển thị danh sách chi tiết (List View)
                val detailList = if (showTypeDetail == TransactionType.INCOME) incomeTransactions else expenseTransactions
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    item { 
                        Text(
                            "Danh sách giao dịch (${detailList.size})", 
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

    // Modal BottomSheet / Dialog giải thích chi tiết Điểm sức khỏe tài chính
    if (showHealthDialog) {
        FinancialHealthDetailDialog(healthResult) { showHealthDialog = false }
    }
}

@Composable
fun AnalyticsTabHeader(
    currentTab: AnalyticsViewTab,
    onTabSelected: (AnalyticsViewTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        AnalyticsViewTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) PrimaryBlue else Color.Transparent)
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tab.iconEmoji, fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Financial Health Score Card ─────────────────────────────────────────────

@Composable
fun FinancialHealthOverviewCard(
    result: FinancialHealthResult,
    onClick: () -> Unit
) {
    val scoreColor = when {
        result.totalScore >= 85 -> AccentGreen
        result.totalScore >= 70 -> PrimaryBlue
        result.totalScore >= 50 -> Color(0xFFF59E0B)
        else                    -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gauge Score
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(scoreColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { result.totalScore / 100f },
                    modifier = Modifier.size(68.dp),
                    color = scoreColor,
                    strokeWidth = 6.dp,
                    trackColor = scoreColor.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${result.totalScore}",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = scoreColor
                    )
                    Text("/100", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sức khỏe tài chính", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(scoreColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            result.grade,
                            color = scoreColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    result.summary,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 2
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Dialog Chi tiết Sức khỏe Tài chính ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialHealthDetailDialog(
    result: FinancialHealthResult,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Đánh giá Sức khỏe Tài chính", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("Điểm tổng hợp: ${result.totalScore}/100 • ${result.grade}", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                result.summary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))
            Text("Chi tiết 4 trụ cột tài chính", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))

            result.pillars.forEach { pillar ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pillar.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${pillar.score}/${pillar.maxScore} đ", fontWeight = FontWeight.Black, color = PrimaryBlue, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { pillar.score.toFloat() / pillar.maxScore },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (pillar.score >= 20) AccentGreen else if (pillar.score >= 14) PrimaryBlue else Color(0xFFEF4444),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(pillar.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text("💡 Lời khuyên: ${pillar.advice}", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── Period Comparison Card ──────────────────────────────────────────────────

@Composable
fun ComparisonModeSelector(
    selected: ComparisonMode,
    onSelect: (ComparisonMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(4.dp)
    ) {
        ComparisonMode.values().forEach { mode ->
            val isSelected = selected == mode
            val label = when(mode) {
                ComparisonMode.MONTH_OVER_MONTH -> "Tháng"
                ComparisonMode.QUARTER_OVER_QUARTER -> "Quý"
                ComparisonMode.WEEK_OVER_WEEK -> "Tuần"
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) PrimaryBlue else Color.Transparent)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PeriodComparisonCard(comparison: PeriodComparisonResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("So sánh ${comparison.currentPeriodLabel} vs ${comparison.previousPeriodLabel}", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Tổng quan tăng giảm
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(comparison.currentPeriodLabel, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(formatCurrency(comparison.currentTotalExpense), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(comparison.previousPeriodLabel, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(formatCurrency(comparison.previousTotalExpense), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Badge tổng chênh lệch
            val isOver = comparison.totalDiffAmount > 0
            val pctStr = String.format("%.1f%%", abs(comparison.totalPercentChange))
            val diffAmtStr = formatCurrency(abs(comparison.totalDiffAmount))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isOver) Color(0xFFEF4444).copy(alpha = 0.1f) else AccentGreen.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isOver) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    null,
                    tint = if (isOver) Color(0xFFEF4444) else AccentGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isOver) "Chi tiêu tăng $pctStr (+$diffAmtStr) so với kỳ trước"
                    else "Chi tiêu giảm $pctStr (-$diffAmtStr) so với kỳ trước",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) Color(0xFFEF4444) else AccentGreen
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Chi tiết theo từng danh mục", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))

            if (comparison.categoryComparisons.isEmpty()) {
                Text("Chưa có đủ dữ liệu so sánh", fontSize = 12.sp, color = Color.Gray)
            } else {
                comparison.categoryComparisons.forEach { catComp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(catComp.category, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "${formatCurrency(catComp.currentAmount)} (trước: ${formatCurrency(catComp.previousAmount)})",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        val isCatOver = catComp.isIncrease
                        val catPctStr = "${if (isCatOver) "+" else "-"}${String.format("%.0f", abs(catComp.percentChange))}%"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCatOver) Color(0xFFEF4444).copy(alpha = 0.12f) else AccentGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                catPctStr,
                                color = if (isCatOver) Color(0xFFEF4444) else AccentGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─── Day of Week Analysis Card ────────────────────────────────────────────────

@Composable
fun DayOfWeekAnalysisCard(analysis: DayOfWeekAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chi tiêu theo Thứ trong tuần", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("TB lịch sử", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(14.dp))

            // AI Insight Text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryBlue.copy(alpha = 0.08f))
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        analysis.insightText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            val maxVal = (analysis.dailyList.maxOfOrNull { it.averageAmount } ?: 1.0).coerceAtLeast(1.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                analysis.dailyList.forEach { item ->
                    val isPeak = analysis.peakDay?.dayLabel == item.dayLabel
                    val hRatio = (item.averageAmount / maxVal).toFloat().coerceIn(0.06f, 1.0f)
                    val barColor = if (isPeak) Color(0xFFF59E0B) else PrimaryBlue

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(100.dp)
                                .width(22.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(hRatio)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(barColor)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            item.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isPeak) FontWeight.Black else FontWeight.Bold,
                            color = if (isPeak) barColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.averageAmount > 0) {
                            Text(
                                "${(item.averageAmount / 1000).toInt()}k",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Top 5 Anomaly Categories Card ───────────────────────────────────────────

@Composable
fun AnomalyCategoriesCard(anomalies: List<AnomalyCategory>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥 Top biến động bất thường", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text("vs TB 3 tháng", fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(Modifier.height(14.dp))

            if (anomalies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentGreen.copy(alpha = 0.08f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🎉 Tuyệt vời! Tháng này không có danh mục nào chi tiêu tăng đột biến bất thường so với 3 tháng gần nhất.",
                        fontSize = 12.sp,
                        color = AccentGreen,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                anomalies.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.category, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "Tháng này: ${formatCurrency(item.currentMonthAmount)} (TB 3T: ${formatCurrency(item.threeMonthAvgAmount)})",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "+${item.spikePercentage.toInt()}%",
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─── Net Cash Flow Trend Card ────────────────────────────────────────────────

@Composable
fun NetCashFlowTrendCard(points: List<NetCashFlowPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dòng tiền ròng (Net Cash Flow)", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Thu trừ Chi", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            val maxNet = points.maxOfOrNull { abs(it.netCashFlow) }?.coerceAtLeast(1.0) ?: 1.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                points.forEach { pt ->
                    val isPositive = pt.netCashFlow >= 0
                    val heightRatio = (abs(pt.netCashFlow) / maxNet).toFloat().coerceIn(0.08f, 0.95f)
                    val barColor = if (isPositive) AccentGreen else Color(0xFFEF4444)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(110.dp)
                                .width(24.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(heightRatio)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(barColor)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(pt.periodLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${if (isPositive) "+" else "-"}${ (abs(pt.netCashFlow) / 1000).toInt() }k",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                }
            }
        }
    }
}

// ─── Existing Reusable Composables ───────────────────────────────────────────

@Composable
fun PeriodSelectorMenu(selected: AnalyticsPeriod, onSelect: (AnalyticsPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        AnalyticsPeriod.values().forEach { p ->
            val isSelected = selected == p
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
                    p.label, 
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
                Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
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
                            Text("${((item.second / total) * 100).toInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = getChartColor(selectedIndex))
                        } else {
                            Text("Tổng chi", fontSize = 11.sp, color = Color.Gray)
                            Text("${(total/1_000).toInt()}k", fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                
                Spacer(Modifier.width(20.dp))
                
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
            val innerRadius = 55.dp.toPx()
            val outerRadius = 95.dp.toPx()
            
            if (radius in innerRadius..outerRadius) {
                var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                if (angle < 0) angle += 360f
                
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
                startAngle = startAngle + 2f,
                sweepAngle = sweepAngle - 4f,
                useCenter = false,
                style = Stroke(width = if (isSelected) 44f else 34f, cap = StrokeCap.Round),
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
                Text("Xu hướng tuần", fontWeight = FontWeight.Black, fontSize = 18.sp)
                
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
            
            Spacer(Modifier.height(32.dp))
            
            val maxVal = (trend.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
            
            Row(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEachIndexed { index, (day, amount) ->
                    val hRatio = (amount / maxVal).toFloat().coerceIn(0.05f, 1.0f)
                    
                    val animatedHeight by animateFloatAsState(
                        targetValue = hRatio,
                        animationSpec = tween(800, delayMillis = index * 40, easing = FastOutSlowInEasing),
                        label = "BarHeight"
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.height(120.dp).width(20.dp),
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
