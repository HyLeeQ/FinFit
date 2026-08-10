package com.example.finfit.insights.domain.model

data class CrossModuleDailyMetric(
    val dayOfWeek: Int,          // 1: T2, 2: T3, ..., 7: CN
    val dayLabel: String,        // "T2", "T3", ...
    val foodExpense: Double,     // Tiền ăn uống (VNĐ)
    val caloriesIn: Int,         // Calo nạp vào (kcal)
    val steps: Int,              // Bước chân
    val isDiningOut: Boolean = false
)

data class CrossModuleWeeklySummary(
    val diningOutExpense: Double = 0.0,
    val homeCookingExpense: Double = 0.0,
    val totalFoodExpense: Double = 0.0,
    val totalCalories: Int = 0,
    val totalProteinGrams: Double = 0.0,
    val costPerThousandCalories: Double = 0.0,   // VNĐ / 1.000 kcal
    val costPerHundredGramProtein: Double = 0.0, // VNĐ / 100g Protein
    val dailyMetrics: List<CrossModuleDailyMetric> = emptyList(),
    val diningOutVsHomeRatio: Double = 0.0,      // % ăn ngoài / tổng tiền ăn
    val weeklyPatternInsight: String = ""        // Nhận định xu hướng cuối tuần vs ngày thường
)

data class HealthySavingsPiggybank(
    val totalVirtualSaved: Double = 0.0,         // Tổng tiền ảo tiết kiệm được từ tự nấu ăn
    val homeCookedMealsCount: Int = 0,           // Số bữa tự nấu
    val diningOutMealsCount: Int = 0,            // Số bữa ăn ngoài
    val averageDiningOutCost: Double = 50000.0,  // Chi phí trung bình mỗi bữa ăn ngoài
    val averageHomeCookedCost: Double = 20000.0, // Chi phí trung bình mỗi bữa tự nấu
    val streakDays: Int = 0                      // Chuỗi ngày duy trì tự nấu ăn
)

data class CrossModuleChallenge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val targetSteps: Int,
    val maxDiningOutCount: Int,
    val currentStepsAvg: Int,
    val currentDiningOutCount: Int,
    val isCompleted: Boolean,
    val rewardBadge: String
)

data class CrossModuleBadge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val unlockedDateStr: String? = null
)

data class CrossModuleAlert(
    val title: String,
    val message: String,
    val severity: String,          // "WARNING", "INFO", "CRITICAL"
    val recommendation: String
)
