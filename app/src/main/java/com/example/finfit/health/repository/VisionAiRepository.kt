package com.example.finfit.health.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.finfit.health.api.vision.VisionAiProvider
import com.example.finfit.health.model.vision.DishNutritionResult
import com.example.finfit.health.model.vision.VisionAiResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Handles Prompt Engineering, network orchestration, and safe JSON extraction.
 * Interacts with the abstract VisionAiProvider to keep business logic isolated.
 */
class VisionAiRepository(
    private val provider: VisionAiProvider
) {
    private val gson = Gson()
    private val firestore = FirebaseFirestore.getInstance()

    fun normalizeDishName(name: String): String {
        val lowercase = name.trim().lowercase(Locale.getDefault())
        val decomposed = java.text.Normalizer.normalize(lowercase, java.text.Normalizer.Form.NFD)
        val regexMarks = Regex("\\p{InCombiningDiacriticalMarks}+")
        var withoutAccents = regexMarks.replace(decomposed, "")
        
        // Custom replacement for Vietnamese specific 'đ'
        withoutAccents = withoutAccents
            .replace("đ", "d")
            .replace("Đ", "d")
            
        // Remove special characters, keep only standard alphanumeric characters and spaces
        val clean = withoutAccents.replace(Regex("[^a-z0-9\\s]"), "")
        
        // Collapse spaces and replace with single underscore
        return clean.trim().replace(Regex("\\s+"), "_")
    }

    /**
     * Check if a dish's nutrition is already cached globally in Firestore.
     */
    suspend fun getGlobalCachedNutrition(dishName: String): DishNutritionResult? {
        val normalizedId = normalizeDishName(dishName)
        if (normalizedId.isEmpty()) return null
        
        return try {
            Log.d("VisionAiRepository", "Checking global Food Knowledge Cache for: [$normalizedId]...")
            val document = firestore.collection("food_knowledge_cache")
                .document(normalizedId)
                .get()
                .await()
            
            if (document.exists()) {
                val name = document.getString("dishName") ?: ""
                val confidence = document.getDouble("dishConfidence")?.toFloat() ?: 1.0f
                val calories = document.getDouble("estimatedCalories")?.toFloat() ?: 0f
                val healthScore = document.getDouble("healthScore")?.toFloat() ?: 0f
                
                val ingredientsList = document.get("ingredients") as? List<Map<String, Any>>
                val ingredients = ingredientsList?.map {
                    com.example.finfit.health.model.vision.IngredientInfo(
                        name = it["name"] as? String ?: "",
                        confidence = (it["confidence"] as? Double)?.toFloat() ?: 1.0f
                    )
                } ?: emptyList()
                
                val possibleList = document.get("possibleDishes") as? List<Map<String, Any>>
                val possibleDishes = possibleList?.map {
                    com.example.finfit.health.model.vision.DishInfo(
                        name = it["name"] as? String ?: "",
                        confidence = (it["confidence"] as? Double)?.toFloat() ?: 1.0f
                    )
                } ?: emptyList()
                
                val macrosMap = document.get("macros") as? Map<String, Any>
                val macros = com.example.finfit.health.model.vision.Macros(
                    proteinG = (macrosMap?.get("proteinG") as? Double)?.toFloat() ?: 0f,
                    carbsG = (macrosMap?.get("carbsG") as? Double)?.toFloat() ?: 0f,
                    fatG = (macrosMap?.get("fatG") as? Double)?.toFloat() ?: 0f
                )
                
                val analysisNotes = document.get("analysisNotes") as? List<String> ?: emptyList()
                
                val cachedResult = DishNutritionResult(
                    dishName = name,
                    dishConfidence = confidence,
                    possibleDishes = possibleDishes,
                    ingredients = ingredients,
                    estimatedCalories = calories,
                    macros = macros,
                    healthScore = healthScore,
                    analysisNotes = analysisNotes
                )
                
                Log.d("VisionAiRepository", "🔥 Cache HIT! Reusing global nutrition data for [$dishName].")
                return cachedResult
            }
            Log.d("VisionAiRepository", "❄️ Cache MISS for: [$normalizedId].")
            null
        } catch (e: Exception) {
            Log.e("VisionAiRepository", "Failed to read from global Food Knowledge Cache", e)
            null
        }
    }

    /**
     * Save a successfully analyzed dish globally for all users to reuse.
     */
    suspend fun saveGlobalCachedNutrition(dishName: String, result: DishNutritionResult) {
        val normalizedId = normalizeDishName(dishName)
        if (normalizedId.isEmpty()) return
        
        try {
            Log.d("VisionAiRepository", "Saving [$dishName] (ID: $normalizedId) to global Food Knowledge Cache...")
            val dataMap = hashMapOf<String, Any>(
                "dishName" to result.dishName,
                "dishConfidence" to result.dishConfidence,
                "estimatedCalories" to result.estimatedCalories,
                "healthScore" to result.healthScore,
                "popularityCount" to 1L,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "ingredients" to result.ingredients.map { mapOf("name" to it.name, "confidence" to it.confidence) },
                "possibleDishes" to result.possibleDishes.map { mapOf("name" to it.name, "confidence" to it.confidence) },
                "macros" to mapOf("proteinG" to result.macros.proteinG, "carbsG" to result.macros.carbsG, "fatG" to result.macros.fatG),
                "analysisNotes" to result.analysisNotes
            )
            
            firestore.collection("food_knowledge_cache")
                .document(normalizedId)
                .set(dataMap)
                .await()
            Log.d("VisionAiRepository", "✅ Stored [$dishName] in global Food Knowledge Cache successfully.")
        } catch (e: Exception) {
            Log.e("VisionAiRepository", "Failed to write to global Food Knowledge Cache", e)
        }
    }

    /**
     * Extracts strict JSON from potentially dirty AI responses.
     * AI models sometimes prepend "```json" or include trailing text.
     */
    private fun extractJsonBlock(rawResponse: String): String {
        val startIndex = rawResponse.indexOf("{")
        val endIndex = rawResponse.lastIndexOf("}")
        
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            Log.e("VisionAiRepository", "Raw output does not contain JSON: $rawResponse")
            throw Exception("AI trả về định dạng không hợp lệ (Không tìm thấy JSON).")
        }
        
        return rawResponse.substring(startIndex, endIndex + 1)
    }

    suspend fun analyzeFood(bitmap: Bitmap): VisionAiResult<DishNutritionResult> = withContext(Dispatchers.IO) {
        try {
            Log.d("VisionAiRepository", "Initiating Advanced Unified AI Reasoning Pipeline...")
            
            // Unified Step: Execute 1 Deep Gemini Analysis Prompt
            Log.d("VisionAiRepository", "Executing Unified Deep Gemini Analysis...")
            
            val deepPrompt = """
                Bạn là chuyên gia dinh dưỡng và nhận diện thực phẩm chuyên nghiệp, đặc biệt am hiểu ẩm thực Việt Nam và quốc tế.
                Hãy phân tích hình ảnh được cung cấp và thực hiện suy luận theo nhiều bước:
                1. Xác định món ăn chính và các lựa chọn thay thế có thể có.
                2. Xác định tất cả nguyên liệu có thể nhìn thấy và đánh giá mức độ tin cậy từng nguyên liệu.
                3. Tính toán giá trị dinh dưỡng dựa trên khẩu phần tiêu chuẩn.
                4. Kiểm tra xem tên món ăn có nhất quán với các nguyên liệu được phát hiện không.
                
                BẮT BUỘC trả về kết quả ĐÚNG ĐỊNH DẠNG JSON sau:
                {
                  "dish_name": "Tên món ăn cụ thể bằng tiếng Việt (ví dụ: 'Phở Bò', 'Cơm Tấm', 'Bún Chả')",
                  "dish_confidence": 0.95,
                  "possible_dishes": [
                    {"name": "Tên món khả năng cao nhất", "confidence": 0.95},
                    {"name": "Tên món khả năng thứ hai", "confidence": 0.70}
                  ],
                  "ingredients": [
                    {"name": "Tên nguyên liệu bằng tiếng Việt", "confidence": 0.98}
                  ],
                  "estimated_calories": 500.0,
                  "macros": {
                    "protein_g": 30.0,
                    "carbs_g": 50.0,
                    "fat_g": 20.0
                  },
                  "health_score": 8.5,
                  "analysis_notes": [
                    "Giàu protein",
                    "Chứa các chất gây dị ứng phổ biến như tôm"
                  ]
                }
                
                QUY TẮC:
                - TẤT CẢ giá trị văn bản (dish_name, possible_dishes[].name, ingredients[].name, analysis_notes) PHẢI bằng tiếng Việt.
                - Ưu tiên tên tiếng Việt cho món ăn Việt Nam.
                - Trung thực với điểm tin cậy. Nếu không chắc, hãy giảm điểm.
                - Nếu hình ảnh không phải thực phẩm, trả về JSON với dish_name="Không xác định" và confidence=0.0.
                - KHÔNG trả về bất kỳ định dạng markdown nào, không có khối ```json, không có văn bản trước hoặc sau JSON.
            """.trimIndent()

            var rawResponse: String? = null
            var lastError: Exception? = null
            
            for (i in 1..3) {
                try {
                    rawResponse = provider.analyzeImage(bitmap, deepPrompt)
                    if (rawResponse.isNotEmpty()) break
                } catch (e: Exception) {
                    lastError = e
                    if (e.message?.contains("503") == true || e.message?.contains("UNAVAILABLE") == true) {
                        Log.w("VisionAiRepository", "API Busy (503), retrying $i/3...")
                        kotlinx.coroutines.delay(1500L * i)
                        continue
                    }
                    throw e
                }
            }

            if (rawResponse == null) {
                val userMessage = when {
                    lastError?.message?.contains("503") == true -> "Máy chủ AI hiện đang quá tải. Vui lòng thử lại sau vài giây."
                    lastError?.message?.contains("429") == true -> "Bạn đã gửi quá nhiều yêu cầu. Vui lòng đợi một lát."
                    else -> "Lỗi kết nối AI: ${lastError?.localizedMessage}"
                }
                return@withContext VisionAiResult.Error(userMessage)
            }

            Log.d("VisionAiRepository", "Raw AI Output: $rawResponse")

            val jsonString = try {
                extractJsonBlock(rawResponse)
            } catch (e: Exception) {
                return@withContext VisionAiResult.Error(e.message ?: "Lỗi trích xuất dữ liệu.")
            }

            val rawResult = try {
                gson.fromJson(jsonString, DishNutritionResult::class.java)
            } catch (e: Exception) {
                Log.e("VisionAiRepository", "JSON Parsing Error", e)
                return@withContext VisionAiResult.Error("Lỗi cấu trúc dữ liệu AI. Vui lòng thử lại.")
            }

            if (rawResult == null || rawResult.dishName.isEmpty() || rawResult.dishName == "Không xác định") {
                return@withContext VisionAiResult.Error("AI không xác định được món ăn này.")
            }

            // Heuristic confidence validation
            val validatedResult = applyHeuristicValidation(rawResult)
            
            Log.d("VisionAiRepository", "Validation Complete. Confidence: ${validatedResult.dishConfidence}")

            if (validatedResult.dishConfidence < 0.25f) {
                return@withContext VisionAiResult.Error("Hệ thống không tự tin nhận diện món ăn này. Vui lòng chụp rõ hơn.")
            }

            // Global cache lookup for data consistency & popularity updates
            val normalizedId = normalizeDishName(validatedResult.dishName)
            val cached = getGlobalCachedNutrition(normalizedId)
            
            if (cached != null) {
                // Update popularity count in background
                try {
                    firestore.collection("food_knowledge_cache")
                        .document(normalizedId)
                        .update("popularityCount", com.google.firebase.firestore.FieldValue.increment(1))
                } catch (e: Exception) {
                    Log.w("VisionAiRepository", "Failed to increment popularity", e)
                }
                Log.d("VisionAiRepository", "🔥 Cache HIT (Unified Pipeline)! Consistent data returned for [${validatedResult.dishName}].")
                return@withContext VisionAiResult.Success(cached)
            }

            // Save globally for future hits
            saveGlobalCachedNutrition(validatedResult.dishName, validatedResult)

            VisionAiResult.Success(validatedResult)
        } catch (e: Exception) {
            Log.e("VisionAiRepository", "Pipeline Failure", e)
            val friendlyError = when {
                e.message?.contains("API Key không hợp lệ") == true -> e.message!!
                e.message?.contains("hạn mức") == true || e.message?.contains("429") == true -> e.message!!
                e.message?.contains("503") == true -> "Máy chủ AI quá tải. Vui lòng thử lại sau vài giây."
                else -> "Lỗi phân tích: ${e.localizedMessage ?: "Mất kết nối với AI"}"
            }
            VisionAiResult.Error(friendlyError, e)
        }
    }

    /**
     * Confidence Engine & Validation Layer
     * Penalizes results for logical inconsistencies or low component confidence.
     */
    private fun applyHeuristicValidation(result: DishNutritionResult): DishNutritionResult {
        var penalty = 0f
        
        // 1. Kiểm tra mâu thuẫn nguyên liệu (Heuristic đơn giản)
        val dishLower = result.dishName.lowercase()
        val hasSeafood = result.ingredients.any {
            val n = it.name.lowercase()
            n.contains("tôm") || n.contains("cá") || n.contains("shrimp") || n.contains("fish")
        }
        val isMeatDish = dishLower.contains("bò") || dishLower.contains("heo") || dishLower.contains("gà") ||
            dishLower.contains("beef") || dishLower.contains("pork") || dishLower.contains("chicken")
        
        if (isMeatDish && hasSeafood) {
            penalty += 0.4f
            Log.w("VisionAiRepository", "Cảnh báo xác thực: Phát hiện mâu thuẫn món/nguyên liệu (Thịt vs Hải sản)")
        }

        // 2. Kiểm tra độ tin cậy trung bình của nguyên liệu
        val avgIngredientConf = if (result.ingredients.isNotEmpty()) {
            result.ingredients.map { it.confidence }.average().toFloat()
        } else 0f
        
        if (avgIngredientConf < 0.6f) {
            penalty += 0.2f
        }

        // 3. Kiểm tra tính nhất quán Calorie/Macro
        val calculatedCals = (result.macros.proteinG * 4) + (result.macros.carbsG * 4) + (result.macros.fatG * 9)
        val calDiff = Math.abs(calculatedCals - result.estimatedCalories)
        if (calDiff > 100) {
            penalty += 0.1f
            Log.w("VisionAiRepository", "Cảnh báo xác thực: Mâu thuẫn Calorie/Macro")
        }

        val finalConfidence = (result.dishConfidence - penalty).coerceIn(0f, 1f)
        return result.copy(
            dishConfidence = finalConfidence,
            analysisNotes = if (penalty > 0) result.analysisNotes + "Lưu ý: AI phát hiện mâu thuẫn logic trong dữ liệu." else result.analysisNotes
        )
    }

}
