package com.example.finfit.health.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.annotations.SerializedName

// Models
data class EdamamNutritionResponse(
    @SerializedName("calories") val calories: Int,
    @SerializedName("totalWeight") val totalWeight: Double,
    @SerializedName("totalNutrients") val totalNutrients: Nutrients?
)

data class Nutrients(
    @SerializedName("CHOCDF") val carbs: NutrientInfo?,
    @SerializedName("PROCNT") val protein: NutrientInfo?,
    @SerializedName("FAT") val fat: NutrientInfo?
)

data class NutrientInfo(
    @SerializedName("label") val label: String,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("unit") val unit: String
)

// API Interface
interface EdamamApiService {
    @GET("api/nutrition-data")
    suspend fun getNutritionData(
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Query("ingr") ingredient: String, // e.g., "150g apple"
        @Query("nutrition-type") nutritionType: String = "logging"
    ): EdamamNutritionResponse
}

// Retrofit Client Builder
object EdamamClient {
    private const val BASE_URL = "https://api.edamam.com/"
    
    // TODO: Replace with real credentials when ready
    const val APP_ID = "00f19776"
    const val APP_KEY = "8a795dc278ab540c65a6561eca76d823"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: EdamamApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EdamamApiService::class.java)
    }

    // Helper map Vietnamese foods to standard English terms or descriptions for Edamam
    fun mapVietnameseLabelToEnglish(label: String): String {
        return when (label) {
            "pho_vietnam" -> "pho soup"
            "bread_vietnam" -> "banh mi sandwich"
            "goi_cuon" -> "spring roll"
            "bun_bo_hue" -> "spicy beef noodle soup"
            else -> label.replace("_", " ")
        }
    }
}
