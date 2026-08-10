package com.example.finfit.finance.ui.logic

import androidx.compose.ui.graphics.Color
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.TransactionType
import java.util.Calendar
import kotlin.math.abs

// ─── Kỳ hạn phân tích ────────────────────────────────────────────────────────

enum class AnalyticsPeriod(val label: String) {
    DAY("Ngày"),
    WEEK("Tuần"),
    MONTH("Tháng"),
    YEAR("Năm")
}

enum class ComparisonMode(val label: String) {
    MONTH_OVER_MONTH("Tháng này vs Tháng trước"),
    QUARTER_OVER_QUARTER("Quý này vs Quý trước"),
    WEEK_OVER_WEEK("Tuần này vs Tuần trước")
}

// ─── So sánh ngày / tuần / tháng / năm ───────────────────────────────────────

fun isSameDay(c1: Calendar, c2: Calendar) =
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

fun isSameWeek(c1: Calendar, c2: Calendar) =
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
    c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR)

fun isSameMonth(c1: Calendar, c2: Calendar) =
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
    c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)

fun isSameYear(c1: Calendar, c2: Calendar) =
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)

// ─── Tính xu hướng chi tiêu theo ngày trong tuần ─────────────────────────────

data class DayOfWeekSpending(
    val dayLabel: String, // T2, T3...
    val dayIndex: Int,    // 0 -> 6 (Monday -> Sunday)
    val totalAmount: Double,
    val averageAmount: Double,
    val count: Int
)

data class DayOfWeekAnalysisResult(
    val dailyList: List<DayOfWeekSpending>,
    val peakDay: DayOfWeekSpending?,
    val averageOtherDays: Double,
    val ratioMultiplier: Double,
    val insightText: String
)

/**
 * Phân tích chi tiêu theo từng thứ trong tuần (T2 -> CN) trên toàn bộ lịch sử
 */
fun analyzeDayOfWeekSpending(transactions: List<FinanceTransaction>): DayOfWeekAnalysisResult {
    val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    val calDays = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )

    val expenseTxs = transactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT }
    val tempCal = Calendar.getInstance()

    val dailyStats = labels.mapIndexed { idx, label ->
        val targetCalDay = calDays[idx]
        val txsForDay = expenseTxs.filter {
            tempCal.time = it.timestamp.toDate()
            tempCal.get(Calendar.DAY_OF_WEEK) == targetCalDay
        }
        val total = txsForDay.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
        // Đếm số ngày xuất hiện độc lập
        val distinctDates = txsForDay.map {
            tempCal.time = it.timestamp.toDate()
            "${tempCal.get(Calendar.YEAR)}_${tempCal.get(Calendar.DAY_OF_YEAR)}"
        }.distinct().size.coerceAtLeast(1)

        val avg = if (total > 0) total / distinctDates else 0.0
        DayOfWeekSpending(label, idx, total, avg, txsForDay.size)
    }

    val peakDay = dailyStats.maxByOrNull { it.averageAmount }
    val otherDays = dailyStats.filter { it.dayLabel != (peakDay?.dayLabel ?: "") }
    val otherAvg = if (otherDays.isNotEmpty()) otherDays.map { it.averageAmount }.average() else 0.0
    val multiplier = if (otherAvg > 0 && peakDay != null) peakDay.averageAmount / otherAvg else 1.0

    val insight = if (peakDay != null && multiplier >= 1.3) {
        val dayFullName = when (peakDay.dayLabel) {
            "T2" -> "Thứ Hai"; "T3" -> "Thứ Ba"; "T4" -> "Thứ Tư"
            "T5" -> "Thứ Năm"; "T6" -> "Thứ Sáu"; "T7" -> "Thứ Bảy"; else -> "Chủ Nhật"
        }
        "Vào **$dayFullName**, bạn chi tiêu trung bình gấp **${String.format("%.1f", multiplier)}x** các ngày khác trong tuần. Hãy lưu ý các khoản ăn uống, giải trí cuối tuần!"
    } else {
        "Mức chi tiêu các ngày trong tuần của bạn khá đồng đều, không có ngày nào đột biến bất thường."
    }

    return DayOfWeekAnalysisResult(dailyStats, peakDay, otherAvg, multiplier, insight)
}

/**
 * Tính tổng chi tiêu theo từng ngày trong tuần cụ thể
 */
fun calculateDailyTrend(
    transactions: List<FinanceTransaction>,
    weekOffset: Int
): List<Pair<String, Double>> {
    val trend = mutableListOf<Pair<String, Double>>()
    val now = Calendar.getInstance()
    now.add(Calendar.WEEK_OF_YEAR, weekOffset)

    val startOfWeek = now.clone() as Calendar
    startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    for (i in 0..6) {
        val d = startOfWeek.clone() as Calendar
        d.add(Calendar.DAY_OF_YEAR, i)

        val dayLabel = when (d.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY    -> "T2"
            Calendar.TUESDAY   -> "T3"
            Calendar.WEDNESDAY -> "T4"
            Calendar.THURSDAY  -> "T5"
            Calendar.FRIDAY    -> "T6"
            Calendar.SATURDAY  -> "T7"
            else               -> "CN"
        }

        val total = transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { time = tx.timestamp.toDate() }
            (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT) &&
                isSameDay(txCal, d)
        }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }

        trend.add(dayLabel to total)
    }
    return trend
}

// ─── So sánh kỳ với kỳ (Period over Period) ───────────────────────────────────

data class CategoryComparison(
    val category: String,
    val currentAmount: Double,
    val previousAmount: Double,
    val diffAmount: Double,       // current - previous
    val percentChange: Double,    // % tăng/giảm
    val isIncrease: Boolean
)

data class PeriodComparisonResult(
    val currentPeriodLabel: String,
    val previousPeriodLabel: String,
    val currentTotalExpense: Double,
    val previousTotalExpense: Double,
    val totalDiffAmount: Double,
    val totalPercentChange: Double,
    val currentTotalIncome: Double,
    val previousTotalIncome: Double,
    val categoryComparisons: List<CategoryComparison>
)

/**
 * Tính so sánh chi tiêu giữa 2 kỳ
 */
fun calculatePeriodComparison(
    transactions: List<FinanceTransaction>,
    mode: ComparisonMode
): PeriodComparisonResult {
    val now = Calendar.getInstance()
    val tempCal = Calendar.getInstance()

    val (currentTxs, prevTxs, curLabel, prevLabel) = when (mode) {
        ComparisonMode.MONTH_OVER_MONTH -> {
            val curMonth = now.get(Calendar.MONTH)
            val curYear = now.get(Calendar.YEAR)
            val prevCal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            val prevMonth = prevCal.get(Calendar.MONTH)
            val prevYear = prevCal.get(Calendar.YEAR)

            val cur = transactions.filter {
                tempCal.time = it.timestamp.toDate()
                tempCal.get(Calendar.MONTH) == curMonth && tempCal.get(Calendar.YEAR) == curYear
            }
            val prev = transactions.filter {
                tempCal.time = it.timestamp.toDate()
                tempCal.get(Calendar.MONTH) == prevMonth && tempCal.get(Calendar.YEAR) == prevYear
            }
            Quadruple(cur, prev, "Tháng ${curMonth + 1}", "Tháng ${prevMonth + 1}")
        }
        ComparisonMode.QUARTER_OVER_QUARTER -> {
            val curQuarter = now.get(Calendar.MONTH) / 3
            val curYear = now.get(Calendar.YEAR)
            val prevQuarterCal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -3) }
            val prevQuarter = prevQuarterCal.get(Calendar.MONTH) / 3
            val prevYear = prevQuarterCal.get(Calendar.YEAR)

            val cur = transactions.filter {
                tempCal.time = it.timestamp.toDate()
                tempCal.get(Calendar.MONTH) / 3 == curQuarter && tempCal.get(Calendar.YEAR) == curYear
            }
            val prev = transactions.filter {
                tempCal.time = it.timestamp.toDate()
                tempCal.get(Calendar.MONTH) / 3 == prevQuarter && tempCal.get(Calendar.YEAR) == prevYear
            }
            Quadruple(cur, prev, "Quý ${curQuarter + 1}", "Quý ${prevQuarter + 1}")
        }
        ComparisonMode.WEEK_OVER_WEEK -> {
            val curWeek = now.get(Calendar.WEEK_OF_YEAR)
            val curYear = now.get(Calendar.YEAR)
            val prevWeekCal = (now.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, -1) }
            val prevWeek = prevWeekCal.get(Calendar.WEEK_OF_YEAR)
            val prevYear = prevWeekCal.get(Calendar.YEAR)

            val cur = transactions.filter {
                tempCal.time = it.timestamp.toDate()
                tempCal.get(Calendar.WEEK_OF_YEAR) == curWeek && tempCal.get(Calendar.YEAR) == curYear
            }
            val prev = transactions.filter {
                tempCal.time = it.timestamp.toDate()
                tempCal.get(Calendar.WEEK_OF_YEAR) == prevWeek && tempCal.get(Calendar.YEAR) == prevYear
            }
            Quadruple(cur, prev, "Tuần này", "Tuần trước")
        }
    }

    val curExpenses = currentTxs.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT }
    val prevExpenses = prevTxs.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT }

    val curTotalExp = curExpenses.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
    val prevTotalExp = prevExpenses.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }

    val curTotalInc = currentTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val prevTotalInc = prevTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    val diffTotal = curTotalExp - prevTotalExp
    val percentTotal = if (prevTotalExp > 0) (diffTotal / prevTotalExp) * 100.0 else if (curTotalExp > 0) 100.0 else 0.0

    // Gom theo từng danh mục
    val allCategories = (curExpenses.map { it.category } + prevExpenses.map { it.category }).distinct()
    val categoryList = allCategories.map { cat ->
        val curCatAmt = curExpenses.filter { it.category == cat }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
        val prevCatAmt = prevExpenses.filter { it.category == cat }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
        val diff = curCatAmt - prevCatAmt
        val pct = if (prevCatAmt > 0) (diff / prevCatAmt) * 100.0 else if (curCatAmt > 0) 100.0 else 0.0

        CategoryComparison(
            category = cat.ifBlank { "Khác" },
            currentAmount = curCatAmt,
            previousAmount = prevCatAmt,
            diffAmount = diff,
            percentChange = pct,
            isIncrease = diff > 0
        )
    }.sortedByDescending { abs(it.diffAmount) }

    return PeriodComparisonResult(
        currentPeriodLabel = curLabel,
        previousPeriodLabel = prevLabel,
        currentTotalExpense = curTotalExp,
        previousTotalExpense = prevTotalExp,
        totalDiffAmount = diffTotal,
        totalPercentChange = percentTotal,
        currentTotalIncome = curTotalInc,
        previousTotalIncome = prevTotalInc,
        categoryComparisons = categoryList
    )
}

// ─── Top 5 danh mục biến động bất thường ─────────────────────────────────────

data class AnomalyCategory(
    val category: String,
    val currentMonthAmount: Double,
    val threeMonthAvgAmount: Double,
    val spikePercentage: Double,
    val excessAmount: Double
)

/**
 * Tự động phát hiện top danh mục có chi tiêu tăng đột biến so với trung bình 3 tháng gần nhất
 */
fun detectAnomalyCategories(transactions: List<FinanceTransaction>): List<AnomalyCategory> {
    val now = Calendar.getInstance()
    val curMonth = now.get(Calendar.MONTH)
    val curYear = now.get(Calendar.YEAR)
    val tempCal = Calendar.getInstance()

    val expenseTxs = transactions.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT }

    // Chi tiêu tháng hiện tại
    val currentMonthTxs = expenseTxs.filter {
        tempCal.time = it.timestamp.toDate()
        tempCal.get(Calendar.MONTH) == curMonth && tempCal.get(Calendar.YEAR) == curYear
    }

    // Chi tiêu 3 tháng trước đó
    val past3MonthsTxs = expenseTxs.filter {
        tempCal.time = it.timestamp.toDate()
        val m = tempCal.get(Calendar.MONTH)
        val y = tempCal.get(Calendar.YEAR)
        val monthDiff = (curYear - y) * 12 + (curMonth - m)
        monthDiff in 1..3
    }

    val distinctCategories = currentMonthTxs.map { it.category }.distinct()
    val anomalies = mutableListOf<AnomalyCategory>()

    for (cat in distinctCategories) {
        val curAmount = currentMonthTxs.filter { it.category == cat }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
        val pastAmount = past3MonthsTxs.filter { it.category == cat }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
        val avgPastAmount = pastAmount / 3.0

        if (curAmount > avgPastAmount && curAmount >= 100000.0) {
            val spikePct = if (avgPastAmount > 0) ((curAmount - avgPastAmount) / avgPastAmount) * 100.0 else 100.0
            val excess = curAmount - avgPastAmount
            if (spikePct >= 20.0 || excess >= 300000.0) {
                anomalies.add(AnomalyCategory(cat.ifBlank { "Khác" }, curAmount, avgPastAmount, spikePct, excess))
            }
        }
    }

    return anomalies.sortedByDescending { it.excessAmount }.take(5)
}

// ─── Biểu đồ dòng tiền ròng theo thời gian (Net Cash Flow) ───────────────────

data class NetCashFlowPoint(
    val periodLabel: String,
    val income: Double,
    val expense: Double,
    val netCashFlow: Double // income - expense
)

/**
 * Tính xu hướng dòng tiền ròng qua 6 tháng gần nhất
 */
fun calculateNetCashFlowHistory(transactions: List<FinanceTransaction>): List<NetCashFlowPoint> {
    val points = mutableListOf<NetCashFlowPoint>()
    val now = Calendar.getInstance()
    val tempCal = Calendar.getInstance()

    for (i in 5 downTo 0) {
        val targetMonthCal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
        val m = targetMonthCal.get(Calendar.MONTH)
        val y = targetMonthCal.get(Calendar.YEAR)
        val label = "T${m + 1}"

        val txsForMonth = transactions.filter {
            tempCal.time = it.timestamp.toDate()
            tempCal.get(Calendar.MONTH) == m && tempCal.get(Calendar.YEAR) == y
        }

        val inc = txsForMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val exp = txsForMonth.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT }
            .sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }

        points.add(NetCashFlowPoint(label, inc, exp, inc - exp))
    }

    return points
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ─── Màu biểu đồ Donut ───────────────────────────────────────────────────────

/** Trả về màu tương ứng với index trong biểu đồ donut/legend. */
fun getChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFF06B6D4), Color(0xFFF97316), Color(0xFF14B8A6),
        Color(0xFF6366F1), Color(0xFF84CC16), Color(0xFFA855F7)
    )
    return colors[index % colors.size]
}
