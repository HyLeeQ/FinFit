package com.example.finfit.health.domain.model

data class StepData(
    val steps: Int = 0,
    val calories: Int = 0,
    val distanceMeters: Double = 0.0,
    val date: String = ""
)

data class WaterLog(
    val id: String = "",
    val amountMl: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class SleepSession(
    val id: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val qualityScore: Int = 0
)

data class FoodMeal(
    val id: String = "",
    val name: String = "",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val mealType: String = "LUNCH",
    val timestamp: Long = System.currentTimeMillis(),
    val linkedTransactionId: String? = null, // Liên kết tới giao dịch chi tiêu Finance
    val costEstimate: Double? = null,        // Chi phí của bữa ăn này (VNĐ)
    val isHomeCooked: Boolean = false        // Tự nấu ăn hay ăn ngoài
)
