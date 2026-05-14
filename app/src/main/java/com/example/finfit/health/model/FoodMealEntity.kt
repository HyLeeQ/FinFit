package com.example.finfit.health.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * MealEntity — Đại diện cho một phiên ăn uống (Bữa ăn).
 * Matches: users/{userId}/health_history/{date}/meals/{mealId}
 */
data class FoodMealEntity(
    val id: String = "",
    val mealName: String = "",
    val totalCalories: Int = 0,
    val totalProtein: Int = 0,
    val totalCarbs: Int = 0,
    val totalFat: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val previewImageUrl: String = "",
    val source: String = "gemini"
)

