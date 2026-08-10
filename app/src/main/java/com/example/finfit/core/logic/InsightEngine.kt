package com.example.finfit.core.logic

import com.example.finfit.core.model.HealthFinanceInsight
import com.example.finfit.core.model.InsightPriority
import com.example.finfit.core.model.InsightType
import com.example.finfit.core.navigation.Routes
import com.example.finfit.finance.model.FinanceBudget
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.SavingsGoal
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.health.model.HealthUiState
import java.util.Calendar

/**
 * InsightEngine — Pure object để sinh ra các Insight liên ngành Sức khỏe + Tài chính.
 *
 * Design:
 * - Không có side effects (pure function)
 * - Dễ unit test
 * - Không phụ thuộc vào Android framework
 *
 * Gọi từ ViewModel hoặc Composable (trong remember block).
 */
object InsightEngine {

    /**
     * Sinh danh sách insights từ dữ liệu sức khỏe và tài chính.
     * Kết quả đã được sắp xếp theo Priority (HIGH → MEDIUM → LOW).
     */
    fun generateInsights(
        healthState: HealthUiState,
        transactions: List<FinanceTransaction>,
        budgets: List<FinanceBudget>,
        goals: List<SavingsGoal>
    ): List<HealthFinanceInsight> {
        val insights = mutableListOf<HealthFinanceInsight>()
        val today = Calendar.getInstance()
        val todayTx = transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { time = tx.timestamp.toDate() }
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        }
        val thisMonthTx = transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { time = tx.timestamp.toDate() }
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
        }

        // ─── RULE 1: Ăn nhiều calo + Chi ăn uống vượt ngân sách ────────────────
        val foodBudget = budgets.find { it.category == "Ăn uống" }
        val foodSpentThisMonth = thisMonthTx
            .filter { it.type == TransactionType.EXPENSE && it.category == "Ăn uống" }
            .sumOf { it.amount }
        if (healthState.caloriesIn > 2500 && foodBudget != null && foodSpentThisMonth > foodBudget.amount * 0.8) {
            insights.add(
                HealthFinanceInsight(
                    type = InsightType.FOOD_BUDGET,
                    title = "Ăn nhiều & tốn tiền",
                    description = "Bạn nạp ${healthState.caloriesIn}kcal và đã dùng ${((foodSpentThisMonth / foodBudget.amount) * 100).toInt()}% ngân sách ăn uống. Thử nấu ăn nhà?",
                    financialImpact = -(foodSpentThisMonth - foodBudget.amount).coerceAtLeast(0.0),
                    healthImpact = -5,
                    actionRoute = Routes.FOOD_SCANNER,
                    priority = InsightPriority.HIGH,
                    emoji = "🍜"
                )
            )
        }

        // ─── RULE 2: Vận động tốt mà không mất tiền gym ─────────────────────────
        val gymSpent = thisMonthTx
            .filter { it.type == TransactionType.EXPENSE && (it.category == "Thể thao" || it.category == "Gym") }
            .sumOf { it.amount }
        if (healthState.steps > 8000 && gymSpent == 0.0) {
            insights.add(
                HealthFinanceInsight(
                    type = InsightType.EXERCISE_SAVING,
                    title = "Tiết kiệm tiền gym!",
                    description = "Bạn đi ${healthState.steps.formatStep()} bước hôm nay mà không tốn tiền gym. Tuyệt vời 💪",
                    financialImpact = 200_000.0, // Ước tính tiết kiệm gym/tháng
                    healthImpact = +5,
                    actionRoute = "stepCounter",
                    priority = InsightPriority.MEDIUM,
                    emoji = "🏃"
                )
            )
        }

        // ─── RULE 3: Ngủ ít + Chi tiêu tháng tăng ──────────────────────────────
        val lastMonthExpense = transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { time = tx.timestamp.toDate() }
            val lastMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            cal.get(Calendar.YEAR) == lastMonth.get(Calendar.YEAR) &&
            cal.get(Calendar.MONTH) == lastMonth.get(Calendar.MONTH) &&
            tx.type == TransactionType.EXPENSE
        }.sumOf { it.amount }
        val thisMonthExpense = thisMonthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val spendingIncreased = lastMonthExpense > 0 && thisMonthExpense > lastMonthExpense * 1.2

        if (healthState.sleepHours < 6f && spendingIncreased) {
            val excess = thisMonthExpense - lastMonthExpense
            insights.add(
                HealthFinanceInsight(
                    type = InsightType.SLEEP_PRODUCTIVITY,
                    title = "Ngủ ít, chi tiêu tăng",
                    description = "Chỉ ngủ ${String.format("%.1f", healthState.sleepHours)}h và chi tiêu tăng ${formatVnd(excess)} so tháng trước. Nghỉ ngơi đủ giúp kiểm soát chi tiêu tốt hơn.",
                    financialImpact = -excess,
                    healthImpact = -10,
                    actionRoute = Routes.SLEEP_SCHEDULE,
                    priority = InsightPriority.HIGH,
                    emoji = "😴"
                )
            )
        }

        // ─── RULE 4: Health score cao → gợi ý thưởng tiết kiệm ─────────────────
        if (healthState.totalHealthScore >= 80 && goals.isNotEmpty()) {
            val topGoal = goals.maxByOrNull { it.targetAmount - it.currentAmount }
            insights.add(
                HealthFinanceInsight(
                    type = InsightType.HEALTH_SCORE_BONUS,
                    title = "Sức khỏe xuất sắc! 🎉",
                    description = "Điểm ${healthState.totalHealthScore}/100 — tự thưởng bằng cách nạp thêm vào mục tiêu \"${topGoal?.goalName ?: "tiết kiệm"}\" nhé!",
                    financialImpact = +50_000.0,
                    healthImpact = 0,
                    actionRoute = Routes.SAVINGS_GOALS,
                    priority = InsightPriority.MEDIUM,
                    emoji = "⭐"
                )
            )
        }

        // ─── RULE 5: Uống ít nước + mua nhiều nước ngọt ────────────────────────
        val drinkSpentToday = todayTx
            .filter { it.type == TransactionType.EXPENSE && (it.category.contains("Đồ uống") || it.category.contains("Nước ngọt") || it.note.contains("nước ngọt", ignoreCase = true)) }
            .sumOf { it.amount }
        if (healthState.waterConsumedMl < 1000 && drinkSpentToday > 0) {
            insights.add(
                HealthFinanceInsight(
                    type = InsightType.HYDRATION_SAVING,
                    title = "Uống ít nước, tốn nhiều",
                    description = "Chỉ uống ${healthState.waterConsumedMl}ml nước nhưng tốn ${formatVnd(drinkSpentToday)} cho đồ uống. Uống nước lọc giúp tiết kiệm và tốt hơn.",
                    financialImpact = -drinkSpentToday,
                    healthImpact = -3,
                    actionRoute = Routes.WATER_TRACKER,
                    priority = InsightPriority.MEDIUM,
                    emoji = "💧"
                )
            )
        }

        // ─── RULE 6: Chi tiêu vượt toàn bộ ngân sách ────────────────────────────
        val overBudgets = budgets.filter { budget ->
            val spent = thisMonthTx.filter { it.type == TransactionType.EXPENSE && (budget.category == "Tất cả" || it.category == budget.category) }.sumOf { it.amount }
            spent > budget.amount
        }
        if (overBudgets.isNotEmpty()) {
            val overBudget = overBudgets.first()
            val spent = thisMonthTx.filter { it.type == TransactionType.EXPENSE && (overBudget.category == "Tất cả" || it.category == overBudget.category) }.sumOf { it.amount }
            insights.add(
                HealthFinanceInsight(
                    type = InsightType.OVERSPEND_STRESS,
                    title = "Vượt ngân sách ${overBudget.category}",
                    description = "Đã chi ${formatVnd(spent)} / hạn mức ${formatVnd(overBudget.amount)}. Stress tài chính có thể ảnh hưởng sức khỏe — hãy xem lại.",
                    financialImpact = -(spent - overBudget.amount),
                    healthImpact = -5,
                    actionRoute = Routes.BUDGET,
                    priority = InsightPriority.HIGH,
                    emoji = "⚠️"
                )
            )
        }

        // ─── RULE 7: Chi phí y tế tăng → nhắc phòng ngừa ───────────────────────
        val medicalSpentThisMonth = thisMonthTx
            .filter { it.type == TransactionType.EXPENSE && (it.category == "Sức khỏe" || it.category == "Y tế" || it.category.contains("thuốc", ignoreCase = true)) }
            .sumOf { it.amount }
        if (medicalSpentThisMonth > 200_000) {
            insights.add(
                HealthFinanceInsight(
                    type = InsightType.MEDICAL_PREVENTION,
                    title = "Chi y tế tháng này cao",
                    description = "Tốn ${formatVnd(medicalSpentThisMonth)} cho sức khỏe. Uống đủ nước và ngủ đủ giấc giúp giảm chi phí này.",
                    financialImpact = -medicalSpentThisMonth,
                    healthImpact = 0,
                    actionRoute = Routes.HEALTH_DASHBOARD,
                    priority = InsightPriority.LOW,
                    emoji = "🏥"
                )
            )
        }

        // Sắp xếp theo priority
        return insights.sortedWith(compareBy {
            when (it.priority) {
                InsightPriority.HIGH   -> 0
                InsightPriority.MEDIUM -> 1
                InsightPriority.LOW    -> 2
            }
        })
    }

    // ─── Helper formatters ─────────────────────────────────────────────────────

    private fun Int.formatStep(): String {
        return java.text.NumberFormat.getInstance(java.util.Locale("vi", "VN")).format(this)
    }

    private fun formatVnd(amount: Double): String {
        val fmt = java.text.NumberFormat.getInstance(java.util.Locale("vi", "VN"))
        return "${fmt.format(amount.toLong())}đ"
    }
}
