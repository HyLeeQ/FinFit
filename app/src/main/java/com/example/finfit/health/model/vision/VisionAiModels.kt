package com.example.finfit.health.model.vision

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Strict JSON representation expected from the Vision AI provider.
 * Use @Keep to prevent ProGuard from obfuscating fields when parsing JSON.
 */
@Keep
data class DishNutritionResult(
    @SerializedName("dish_name") val dishName: String = "",
    @SerializedName("dish_confidence") val dishConfidence: Float = 0f,
    @SerializedName("possible_dishes") val possibleDishes: List<DishInfo> = emptyList(),
    @SerializedName("ingredients") val ingredients: List<IngredientInfo> = emptyList(),
    @SerializedName("estimated_calories") val estimatedCalories: Float = 0f,
    @SerializedName("macros") val macros: Macros = Macros(),
    @SerializedName("health_score") val healthScore: Float = 0f,
    @SerializedName("analysis_notes") val analysisNotes: List<String> = emptyList()
)

@Keep
data class DishInfo(
    @SerializedName("name") val name: String = "",
    @SerializedName("confidence") val confidence: Float = 0f
)

@Keep
data class IngredientInfo(
    @SerializedName("name") val name: String = "",
    @SerializedName("confidence") val confidence: Float = 0f
)

@Keep
data class Macros(
    @SerializedName("protein_g") val proteinG: Float = 0f,
    @SerializedName("carbs_g") val carbsG: Float = 0f,
    @SerializedName("fat_g") val fatG: Float = 0f
)

/**
 * Standardized repository response wrapper.
 */
sealed class VisionAiResult<out T> {
    data class Success<T>(val data: T) : VisionAiResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : VisionAiResult<Nothing>()
}
