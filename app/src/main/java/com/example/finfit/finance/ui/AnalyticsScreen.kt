package com.example.finfit.finance.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.AccentGreen
import com.example.finfit.ui.theme.PrimaryBlue
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    transactions: List<FinanceTransaction>,
    onBack: () -> Unit
) {
    val expenseTransactions = remember(transactions) { transactions.filter { it.type == TransactionType.EXPENSE } }
    val incomeTransactions = remember(transactions) { transactions.filter { it.type == TransactionType.INCOME } }
    
    val totalExpense = expenseTransactions.sumOf { it.amount }
    val totalIncome = incomeTransactions.sumOf { it.amount }
    
    // Aggregate by category
    val categoryStats = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Daily trend (last 7 days)
    val dailyTrend = remember(transactions) {
        calculateDailyTrend(transactions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phân tích chi tiêu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Cards
            IncomeExpenseSummary(totalIncome, totalExpense)

            // Spending Breakdown (Donut Chart)
            if (categoryStats.isNotEmpty()) {
                SpendingBreakdownCard(categoryStats, totalExpense)
            }

            // Spending Trend (Bar Chart)
            DailyTrendCard(dailyTrend)
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun IncomeExpenseSummary(income: Double, expense: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SummaryMiniCard(
            modifier = Modifier.weight(1f),
            label = "Thu nhập",
            amount = income,
            color = AccentGreen,
            icon = Icons.AutoMirrored.Filled.TrendingUp
        )
        SummaryMiniCard(
            modifier = Modifier.weight(1f),
            label = "Chi tiêu",
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
                }
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(formatCurrency(amount), fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun SpendingBreakdownCard(stats: List<Pair<String, Double>>, total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Phân bổ chi tiêu", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Donut Chart
                DonutChart(
                    stats = stats,
                    total = total,
                    modifier = Modifier.size(150.dp)
                )
                
                Spacer(Modifier.width(24.dp))
                
                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.take(5).forEachIndexed { index, pair ->
                        LegendItem(pair.first, pair.second/total, getChartColor(index))
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(stats: List<Pair<String, Double>>, total: Double, modifier: Modifier) {
    Canvas(modifier = modifier) {
        var startAngle = -90f
        stats.forEachIndexed { index, pair ->
            val sweepAngle = (pair.second / total).toFloat() * 360f
            drawArc(
                color = getChartColor(index),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun LegendItem(label: String, percentage: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text("$label (${(percentage * 100).toInt()}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DailyTrendCard(trend: List<Pair<String, Double>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Xu hướng 7 ngày qua", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(32.dp))
            
            val maxVal = trend.maxOfOrNull { it.second } ?: 1.0
            
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEach { (day, amount) ->
                    val hRatio = (amount / maxVal).toFloat().coerceAtLeast(0.05f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(hRatio)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryBlue)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(day, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

fun calculateDailyTrend(transactions: List<FinanceTransaction>): List<Pair<String, Double>> {
    val trend = mutableListOf<Pair<String, Double>>()
    
    for (i in 6 downTo 0) {
        val d = Calendar.getInstance()
        d.add(Calendar.DAY_OF_YEAR, -i)
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
            val txCal = Calendar.getInstance()
            txCal.time = tx.timestamp.toDate()
            tx.type == TransactionType.EXPENSE &&
            txCal.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
            txCal.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.amount }
        
        trend.add(dayLabel to total)
    }
    return trend
}

fun getChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), 
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFF06B6D4), Color(0xFFF97316)
    )
    return colors[index % colors.size]
}
