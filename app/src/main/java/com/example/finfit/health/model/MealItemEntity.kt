package com.example.finfit.health.model

/**
 * MealItemEntity — Đại diện cho từng món ăn đơn lẻ trong một bữa ăn.
 * Document nằm trong: meals/{mealId}/items/{itemId}
 */
data class MealItemEntity(
    val id: String = "",
    val itemName: String = "",
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val confidence: Float = 0f,
    val ingredients: List<String> = emptyList(),
    val imageUrl: String = "",
    val source: String = "gemini",
    val createdAt: Long = System.currentTimeMillis()
)
