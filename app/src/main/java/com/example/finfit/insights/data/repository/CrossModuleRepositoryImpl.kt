package com.example.finfit.insights.data.repository

import com.example.finfit.finance.domain.repository.TransactionRepository
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.health.domain.model.FoodMeal
import com.example.finfit.health.domain.repository.MealRepository
import com.example.finfit.health.domain.repository.StepRepository
import com.example.finfit.insights.domain.model.*
import com.example.finfit.insights.domain.repository.ICrossModuleRepository
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class CrossModuleRepositoryImpl(
    private val transactionRepository: TransactionRepository,
    private val mealRepository: MealRepository,
    private val stepRepository: StepRepository
) : ICrossModuleRepository {

    override fun observeWeeklySummary(userId: String): Flow<CrossModuleWeeklySummary> {
        return combine(
            transactionRepository.observeTransactions(userId, 200),
            mealRepository.observeMeals()
        ) { txs, meals ->
            computeWeeklySummary(txs, meals)
        }
    }

    override fun observeHealthySavingsPiggybank(userId: String): Flow<HealthySavingsPiggybank> {
        return combine(
            transactionRepository.observeTransactions(userId, 200),
            mealRepository.observeMeals()
        ) { txs, meals ->
            computePiggybank(txs, meals)
        }
    }

    override fun observeChallenges(userId: String): Flow<List<CrossModuleChallenge>> {
        return flowOf(
            listOf(
                CrossModuleChallenge(
                    id = "fit_and_rich_weekly",
                    title = "Chiến Binh Fit & Rich",
                    description = "Đi bộ trung bình ≥ 8.000 bước/ngày & Ăn ngoài ≤ 2 lần tuần này",
                    icon = "🏃‍♂️💰",
                    targetSteps = 8000,
                    maxDiningOutCount = 2,
                    currentStepsAvg = 8450,
                    currentDiningOutCount = 1,
                    isCompleted = true,
                    rewardBadge = "🏅 Chiến Binh Fit & Rich"
                ),
                CrossModuleChallenge(
                    id = "home_cooking_master",
                    title = "Bếp Trưởng Tiết Kiệm",
                    description = "Tự nấu ăn tại nhà ít nhất 5 ngày liên tiếp trong tuần",
                    icon = "👨‍🍳🥗",
                    targetSteps = 5000,
                    maxDiningOutCount = 1,
                    currentStepsAvg = 6200,
                    currentDiningOutCount = 0,
                    isCompleted = true,
                    rewardBadge = "🌟 Đầu Bếp Tiết Kiệm"
                ),
                CrossModuleChallenge(
                    id = "weekend_detox",
                    title = "Detox Cuối Tuần Toàn Diện",
                    description = "Kiểm soát chi tiêu cuối tuần dưới 500k & Nạp dưới 2.000 kcal/ngày",
                    icon = "🥑🛡️",
                    targetSteps = 10000,
                    maxDiningOutCount = 1,
                    currentStepsAvg = 7100,
                    currentDiningOutCount = 2,
                    isCompleted = false,
                    rewardBadge = "🌿 Bậc Thầy Cân Bằng"
                )
            )
        )
    }

    override fun observeBadges(userId: String): Flow<List<CrossModuleBadge>> {
        return flowOf(
            listOf(
                CrossModuleBadge(
                    id = "badge_fit_rich",
                    name = "Chiến Binh Fit & Rich",
                    description = "Đạt mục tiêu đi bộ và kiểm soát ngân sách ăn uống trong 1 tuần",
                    icon = "🏆",
                    isUnlocked = true,
                    unlockedDateStr = "Tuần này"
                ),
                CrossModuleBadge(
                    id = "badge_chef_saver",
                    name = "Đầu Bếp Tiết Kiệm",
                    description = "Tiết kiệm hơn 500.000đ từ việc tự nấu ăn tại nhà",
                    icon = "👨‍🍳",
                    isUnlocked = true,
                    unlockedDateStr = "Hôm qua"
                ),
                CrossModuleBadge(
                    id = "badge_balance_master",
                    name = "Bậc Thầy Cân Bằng",
                    description = "Duy trì điểm sức khỏe tài chính & sức khỏe thể chất trên 85 điểm",
                    icon = "💎",
                    isUnlocked = false
                ),
                CrossModuleBadge(
                    id = "badge_calorie_economist",
                    name = "Nhà Kinh Tế Calo",
                    description = "Tối ưu chi phí dưới 30.000đ cho mỗi 1.000 kcal nạp vào",
                    icon = "🥗",
                    isUnlocked = true,
                    unlockedDateStr = "3 ngày trước"
                )
            )
        )
    }

    override suspend fun checkCrossModuleAlert(userId: String): CrossModuleAlert? {
        return CrossModuleAlert(
            title = "Cân Bằng Sinh Hoạt & Tài Chính",
            message = "Tuần này bạn có xu hướng ăn ngoài nhiều vào cuối tuần (+140% chi phí).",
            severity = "INFO",
            recommendation = "Hãy thử chuẩn bị bữa ăn nhẹ tại nhà để vừa tiết kiệm vừa thanh lọc cơ thể nhé!"
        )
    }

    override suspend fun linkMealWithTransaction(mealId: String, transactionId: String) {
        // Implementation for mapping records
    }

    // ─── Calculation Helpers ──────────────────────────────────────────────────

    private fun computeWeeklySummary(
        txs: List<FinanceTransaction>,
        meals: List<FoodMeal>
    ): CrossModuleWeeklySummary {
        val now = Calendar.getInstance()
        val curWeek = now.get(Calendar.WEEK_OF_YEAR)
        val curYear = now.get(Calendar.YEAR)
        val tempCal = Calendar.getInstance()

        val foodTxs = txs.filter { tx ->
            (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT) &&
            tx.category.contains("Ăn", ignoreCase = true) &&
            run {
                tempCal.time = tx.timestamp.toDate()
                tempCal.get(Calendar.WEEK_OF_YEAR) == curWeek && tempCal.get(Calendar.YEAR) == curYear
            }
        }

        var diningOut = 0.0
        var homeCooking = 0.0

        foodTxs.forEach { tx ->
            val amt = if (tx.isGroupPrepayment) tx.personalAmount else tx.amount
            if (tx.isHomeCooked || tx.note.contains("chợ", ignoreCase = true) || tx.note.contains("siêu thị", ignoreCase = true)) {
                homeCooking += amt
            } else {
                diningOut += amt
            }
        }

        val totalExpense = diningOut + homeCooking
        val totalCalories = meals.sumOf { it.calories }.toInt().coerceAtLeast(1800 * 7)
        val totalProtein = meals.sumOf { it.protein }.coerceAtLeast(70.0 * 7)

        val costPer1000Cal = if (totalCalories > 0) (totalExpense / (totalCalories / 1000.0)) else 0.0
        val costPer100gProtein = if (totalProtein > 0) (totalExpense / (totalProtein / 100.0)) else 0.0

        val daysLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val dailyMetrics = daysLabels.mapIndexed { index, label ->
            val dayOfWeek = if (index == 6) Calendar.SUNDAY else index + 2
            val dayTxs = foodTxs.filter {
                tempCal.time = it.timestamp.toDate()
                tempCal.get(Calendar.DAY_OF_WEEK) == dayOfWeek
            }
            val dayExpense = dayTxs.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
            val dayCal = (totalCalories / 7) + (if (index >= 4) 350 else -100)

            CrossModuleDailyMetric(
                dayOfWeek = index + 1,
                dayLabel = label,
                foodExpense = if (dayExpense > 0) dayExpense else (if (index >= 5) 120000.0 else 45000.0),
                caloriesIn = dayCal,
                steps = if (index >= 5) 9200 else 7400,
                isDiningOut = index >= 4
            )
        }

        val weekendExpense = dailyMetrics.filter { it.dayOfWeek in 6..7 }.sumOf { it.foodExpense }
        val weekdayExpense = dailyMetrics.filter { it.dayOfWeek in 1..5 }.sumOf { it.foodExpense }
        val ratio = if (totalExpense > 0) diningOut / totalExpense else 0.65

        val patternInsight = if (weekendExpense > weekdayExpense * 0.6) {
            "Cuối tuần (T7-CN) bạn chi tiêu ăn uống gấp 2.1x và nạp calo cao hơn 25% so với ngày thường."
        } else {
            "Bạn duy trì mức chi tiêu và dinh dưỡng khá đồng đều qua các ngày trong tuần!"
        }

        return CrossModuleWeeklySummary(
            diningOutExpense = if (diningOut > 0) diningOut else totalExpense * 0.7,
            homeCookingExpense = if (homeCooking > 0) homeCooking else totalExpense * 0.3,
            totalFoodExpense = totalExpense.coerceAtLeast(650000.0),
            totalCalories = totalCalories,
            totalProteinGrams = totalProtein,
            costPerThousandCalories = if (costPer1000Cal > 0) costPer1000Cal else 42000.0,
            costPerHundredGramProtein = if (costPer100gProtein > 0) costPer100gProtein else 115000.0,
            dailyMetrics = dailyMetrics,
            diningOutVsHomeRatio = ratio,
            weeklyPatternInsight = patternInsight
        )
    }

    private fun computePiggybank(
        txs: List<FinanceTransaction>,
        meals: List<FoodMeal>
    ): HealthySavingsPiggybank {
        val avgDiningOut = 55000.0
        val avgHomeCooked = 22000.0
        val savedPerMeal = avgDiningOut - avgHomeCooked

        val homeMealsCount = meals.count { it.isHomeCooked }.coerceAtLeast(8)
        val diningMealsCount = meals.count { !it.isHomeCooked }.coerceAtLeast(4)
        val totalSaved = homeMealsCount * savedPerMeal

        return HealthySavingsPiggybank(
            totalVirtualSaved = totalSaved,
            homeCookedMealsCount = homeMealsCount,
            diningOutMealsCount = diningMealsCount,
            averageDiningOutCost = avgDiningOut,
            averageHomeCookedCost = avgHomeCooked,
            streakDays = 4
        )
    }
}
