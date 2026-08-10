package com.example.finfit.finance.util

import com.example.finfit.finance.model.*
import java.util.Calendar

data class HealthPillarScore(
    val name: String,
    val score: Int, // 0 - 25
    val maxScore: Int = 25,
    val status: String,
    val description: String,
    val advice: String
)

data class FinancialHealthResult(
    val totalScore: Int, // 0 - 100
    val grade: String,   // Xuất sắc, Tốt, Khá, Cần cải thiện
    val summary: String,
    val pillars: List<HealthPillarScore>
)

object FinancialHealthCalculator {

    fun calculate(
        wallet: AppUserWallet?,
        transactions: List<FinanceTransaction>,
        budgets: List<FinanceBudget>,
        goals: List<SavingsGoal>,
        debtLoans: List<DebtLoan>
    ): FinancialHealthResult {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        // 1. Tính tổng thu và tổng chi tháng này
        val tempCal = Calendar.getInstance()
        val thisMonthIncome: Double = transactions.filter { tx ->
            tx.type == TransactionType.INCOME && isSameMonthAndYear(tx, tempCal, currentMonth, currentYear)
        }.sumOf { it.amount }

        val thisMonthExpense: Double = transactions.filter { tx ->
            (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT) &&
            isSameMonthAndYear(tx, tempCal, currentMonth, currentYear)
        }.sumOf { (if (it.isGroupPrepayment) it.personalAmount else it.amount) }

        // Tính chi tiêu trung bình tháng (tối thiểu 1 tháng hoặc dùng tháng này)
        val avgMonthlyExpense: Double = if (thisMonthExpense > 0.0) thisMonthExpense else 5000000.0

        // ─── TRỤ CỘT 1: Tỷ lệ tiết kiệm (Savings Ratio - Max 25đ) ───
        val totalSavings: Double = (wallet?.generalSavings ?: 0.0) + goals.sumOf { it.currentAmount }
        val savingsRate: Double = if (thisMonthIncome > 0.0) {
            val netSavedThisMonth = (thisMonthIncome - thisMonthExpense).coerceAtLeast(0.0)
            netSavedThisMonth / thisMonthIncome
        } else {
            if (totalSavings > 0.0) 0.15 else 0.0
        }

        val savingsScore = when {
            savingsRate >= 0.25 -> 25
            savingsRate >= 0.20 -> 22
            savingsRate >= 0.15 -> 18
            savingsRate >= 0.10 -> 14
            savingsRate > 0.0   -> 8
            else                -> 4
        }
        val savingsPillar = HealthPillarScore(
            name = "Tỷ lệ Tiết kiệm",
            score = savingsScore,
            status = if (savingsScore >= 20) "Rất tốt" else if (savingsScore >= 14) "Khá" else "Cần tăng thêm",
            description = "Tỷ lệ tiết kiệm/thu nhập đạt ${(savingsRate * 100).toInt()}% (khuyến nghị >= 20%).",
            advice = if (savingsScore >= 20) "Duy trì thói quen trích tiết kiệm đầu tháng tuyệt vời này!"
                     else "Hãy trích trước 15-20% thu nhập ngay khi nhận lương trước khi chi tiêu."
        )

        // ─── TRỤ CỘT 2: Tỷ lệ Nợ/Thu nhập (Debt Ratio - Max 25đ) ───
        val activeDebts: Double = debtLoans.filter { it.type == DebtLoanType.DEBT && !it.isPaid }.sumOf { it.remainingAmount }
        val debtRatio: Double = if (thisMonthIncome > 0.0) activeDebts / thisMonthIncome else if (activeDebts > 0.0) 0.5 else 0.0

        val debtScore = when {
            activeDebts == 0.0  -> 25
            debtRatio <= 0.10   -> 23
            debtRatio <= 0.20   -> 19
            debtRatio <= 0.35   -> 14
            debtRatio <= 0.50   -> 8
            else                -> 3
        }
        val debtPillar = HealthPillarScore(
            name = "Kiểm soát Nợ",
            score = debtScore,
            status = if (debtScore >= 20) "An toàn" else if (debtScore >= 14) "Trung bình" else "Báo động",
            description = if (activeDebts == 0.0) "Bạn không có khoản nợ nào chưa thanh toán."
                          else "Tổng nợ chiếm ${(debtRatio * 100).toInt()}% thu nhập tháng.",
            advice = if (debtScore >= 20) "Bạn đang kiểm soát nợ rất tốt, không bị áp lực trả nợ."
                     else "Ưu tiên phương pháp quả cầu tuyết (trả dứt điểm nợ nhỏ trước) hoặc nợ lãi cao trước."
        )

        // ─── TRỤ CỘT 3: Tuân thủ Ngân sách (Budget Discipline - Max 25đ) ───
        val totalMonthlyBudget: Double = budgets.filter { it.period == BudgetPeriod.MONTHLY && it.category == "Tất cả" }
            .sumOf { it.amount + it.rolloverAmount }
        val budgetScore = if (totalMonthlyBudget > 0.0) {
            val ratio: Double = thisMonthExpense / totalMonthlyBudget
            when {
                ratio <= 0.80 -> 25
                ratio <= 0.95 -> 22
                ratio <= 1.05 -> 17
                ratio <= 1.20 -> 10
                else          -> 4
            }
        } else {
            if (thisMonthExpense <= thisMonthIncome && thisMonthIncome > 0.0) 20 else 15
        }
        val budgetPillar = HealthPillarScore(
            name = "Tuân thủ Ngân sách",
            score = budgetScore,
            status = if (budgetScore >= 20) "Kỷ luật cao" else if (budgetScore >= 15) "Chấp nhận được" else "Vượt hạn mức",
            description = if (totalMonthlyBudget > 0.0) "Đã chi ${(thisMonthExpense / totalMonthlyBudget * 100).toInt()}% hạn mức tháng."
                          else "Chưa thiết lập ngân sách tổng thể.",
            advice = if (totalMonthlyBudget <= 0.0) "Hãy đặt Hạn mức chi tiêu tháng để quản lý dòng tiền chủ động hơn."
                     else if (budgetScore >= 20) "Bạn chi tiêu rất có kỷ luật và luôn nằm trong tầm kiểm soát."
                     else "Hãy giảm các khoản mua sắm ngẫu hứng và áp dụng quy tắc 24h trước khi mua đồ đắt tiền."
        )

        // ─── TRỤ CỘT 4: Quỹ Dự Phòng Khẩn Cấp (Emergency Fund - Max 25đ) ───
        val emergencyReserve: Double = (wallet?.generalSavings ?: 0.0) + (wallet?.emergencyFundBalance ?: 0.0)
        val monthsCovered: Double = if (avgMonthlyExpense > 0.0) emergencyReserve / avgMonthlyExpense else 0.0

        val emergencyScore = when {
            monthsCovered >= 6.0 -> 25
            monthsCovered >= 4.0 -> 22
            monthsCovered >= 3.0 -> 19
            monthsCovered >= 1.5 -> 14
            monthsCovered >= 0.5 -> 8
            else                 -> 3
        }
        val emergencyPillar = HealthPillarScore(
            name = "Quỹ Dự phòng Khẩn cấp",
            score = emergencyScore,
            status = if (emergencyScore >= 20) "Vững chắc" else if (emergencyScore >= 14) "Tạm ổn" else "Thiếu hụt",
            description = "Quỹ dự phòng đủ chi trả khoảng ${String.format("%.1f", monthsCovered)} tháng chi tiêu (khuyến nghị 3-6 tháng).",
            advice = if (emergencyScore >= 20) "Quỹ khẩn cấp vững vàng giúp bạn an tâm trước mọi biến cố."
                     else "Hãy tích lũy đủ 3 tháng chi phí sinh hoạt tối thiểu vào ví Tiết kiệm dự phòng."
        )

        val total = (savingsScore + debtScore + budgetScore + emergencyScore).coerceIn(0, 100)
        val (grade, summary) = when {
            total >= 85 -> "Xuất sắc" to "Sức khỏe tài chính rất khỏe mạnh! Bạn có cơ cấu tài sản vững vàng và kỷ luật chi tiêu tuyệt vời."
            total >= 70 -> "Tốt" to "Tài chính của bạn đang ở trạng thái tốt. Cải thiện thêm quỹ dự phòng hoặc tỷ lệ tiết kiệm để đạt điểm tối đa."
            total >= 50 -> "Trung bình" to "Cần chú ý hơn đến việc kiểm soát hạn mức chi tiêu và tích lũy thêm quỹ dự phòng."
            else        -> "Cần cải thiện" to "Cảnh báo tài chính: Thu chi đang mất cân đối hoặc tỷ lệ nợ cao. Cần lập kế hoạch thắt chặt chi tiêu ngay."
        }

        return FinancialHealthResult(
            totalScore = total,
            grade = grade,
            summary = summary,
            pillars = listOf(savingsPillar, debtPillar, budgetPillar, emergencyPillar)
        )
    }

    private fun isSameMonthAndYear(tx: FinanceTransaction, cal: Calendar, m: Int, y: Int): Boolean {
        cal.time = tx.timestamp.toDate()
        return cal.get(Calendar.MONTH) == m && cal.get(Calendar.YEAR) == y
    }
}
