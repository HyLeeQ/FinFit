package com.example.finfit.finance.ui.logic

import com.example.finfit.finance.model.BudgetPeriod
import com.example.finfit.finance.model.FinanceBudget
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.TransactionType
import java.util.Calendar

data class BudgetPaceResult(
    val spentSoFar: Double,
    val totalBudget: Double,
    val daysElapsed: Int,
    val totalDaysInMonth: Int,
    val dailyPace: Double,               // Chi tiêu trung bình mỗi ngày đã qua
    val projectedMonthEndSpent: Double,  // Dự đoán chi tiêu cả tháng
    val isProjectedToOverspend: Boolean,
    val projectedDifference: Double,     // Số tiền vượt hoặc dư dự kiến
    val paceRatio: Double,               // Tỷ lệ tốc độ (% dự kiến vượt/dư)
    val paceSummary: String              // Lời khuyên/dự đoán bằng chữ
)

object BudgetLogic {

    /**
     * Tính mức chi tiêu trung bình 3 tháng gần nhất của một danh mục
     */
    fun calculate3MonthAverage(
        category: String,
        transactions: List<FinanceTransaction>
    ): Double {
        val now = Calendar.getInstance()
        val curMonth = now.get(Calendar.MONTH)
        val curYear = now.get(Calendar.YEAR)
        val tempCal = Calendar.getInstance()

        val expenseTxs = transactions.filter {
            (it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT) &&
            (category == "Tất cả" || it.category == category)
        }

        val past3MonthsTxs = expenseTxs.filter {
            tempCal.time = it.timestamp.toDate()
            val m = tempCal.get(Calendar.MONTH)
            val y = tempCal.get(Calendar.YEAR)
            val monthDiff = (curYear - y) * 12 + (curMonth - m)
            monthDiff in 1..3
        }

        val totalPast = past3MonthsTxs.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
        val distinctMonths = past3MonthsTxs.map {
            tempCal.time = it.timestamp.toDate()
            "${tempCal.get(Calendar.YEAR)}_${tempCal.get(Calendar.MONTH)}"
        }.distinct().size.coerceAtLeast(1)

        return if (totalPast > 0) totalPast / distinctMonths else 0.0
    }

    /**
     * Tự động gợi ý ngân sách hợp lý (= Trung bình 3 tháng + 10% biên an toàn, làm tròn đến 100k)
     */
    fun suggestBudgetAmount(
        category: String,
        transactions: List<FinanceTransaction>
    ): Double {
        val avg = calculate3MonthAverage(category, transactions)
        if (avg <= 0.0) {
            return if (category == "Tất cả") 10000000.0 else 2000000.0
        }
        val withBuffer = avg * 1.1 // Thêm 10% biên an toàn
        // Làm tròn lên bội số 50.000đ hoặc 100.000đ
        val rounded = (Math.ceil(withBuffer / 50000.0) * 50000.0)
        return rounded
    }

    /**
     * Tính toán tiến độ và dự báo tốc độ chi tiêu so với số ngày trong tháng
     */
    fun calculateSpendingPace(
        budget: FinanceBudget,
        transactions: List<FinanceTransaction>
    ): BudgetPaceResult {
        val now = Calendar.getInstance()
        val curMonth = now.get(Calendar.MONTH)
        val curYear = now.get(Calendar.YEAR)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val maxDaysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

        val tempCal = Calendar.getInstance()
        val spentThisMonth = transactions.filter { tx ->
            (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT) &&
            (budget.category == "Tất cả" || tx.category == budget.category) &&
            run {
                tempCal.time = tx.timestamp.toDate()
                tempCal.get(Calendar.MONTH) == curMonth && tempCal.get(Calendar.YEAR) == curYear
            }
        }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }

        val effectiveBudget = budget.amount + (if (budget.isRollover) budget.rolloverAmount else 0.0)
        val dailyPace = spentThisMonth / dayOfMonth
        val projectedSpent = dailyPace * maxDaysInMonth
        val isOver = projectedSpent > effectiveBudget
        val diff = projectedSpent - effectiveBudget

        val paceSummary = if (effectiveBudget <= 0) {
            "Chưa đặt hạn mức."
        } else if (isOver) {
            val overPct = ((diff / effectiveBudget) * 100).toInt()
            "⚠️ Với tốc độ hiện tại (${formatMoney(dailyPace)}/ngày), dự kiến cuối tháng sẽ vượt ${overPct}% hạn mức (~${formatMoney(diff)})."
        } else {
            val savedPct = (( (effectiveBudget - projectedSpent) / effectiveBudget) * 100).toInt()
            "✅ Bạn đang chi tiêu rất tốt! Dự kiến cuối tháng sẽ còn dư ${savedPct}% hạn mức (~${formatMoney(effectiveBudget - projectedSpent)})."
        }

        return BudgetPaceResult(
            spentSoFar = spentThisMonth,
            totalBudget = effectiveBudget,
            daysElapsed = dayOfMonth,
            totalDaysInMonth = maxDaysInMonth,
            dailyPace = dailyPace,
            projectedMonthEndSpent = projectedSpent,
            isProjectedToOverspend = isOver,
            projectedDifference = diff,
            paceRatio = if (effectiveBudget > 0) projectedSpent / effectiveBudget else 0.0,
            paceSummary = paceSummary
        )
    }

    private fun formatMoney(amount: Double): String {
        return String.format("%,d đ", amount.toLong()).replace(',', '.')
    }
}
