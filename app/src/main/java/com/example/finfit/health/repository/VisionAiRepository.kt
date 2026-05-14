package com.example.finfit.health.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.finfit.health.api.vision.VisionAiProvider
import com.example.finfit.health.model.vision.DishNutritionResult
import com.example.finfit.health.model.vision.VisionAiResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles Prompt Engineering, network orchestration, and safe JSON extraction.
 * Interacts with the abstract VisionAiProvider to keep business logic isolated.
 */
class VisionAiRepository(
    private val provider: VisionAiProvider
) {
    private val gson = Gson()

    /**
     * Extracts strict JSON from potentially dirty AI responses.
     * AI models sometimes prepend "```json" or include trailing text.
     */
    private fun extractJsonBlock(rawResponse: String): String {
        // Find the first '{' and the last '}'
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
            Log.d("VisionAiRepository", "Initiating Advanced AI Reasoning Pipeline...")
            
            val prompt = """
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
            
            // SIMPLE RETRY MECHANISM (3 attempts)
            for (i in 1..3) {
                try {
                    rawResponse = provider.analyzeImage(bitmap, prompt)
                    if (rawResponse.isNotEmpty()) break
                } catch (e: Exception) {
                    lastError = e
                    if (e.message?.contains("503") == true || e.message?.contains("UNAVAILABLE") == true) {
                        Log.w("VisionAiRepository", "API Busy (503), retrying $i/3...")
                        kotlinx.coroutines.delay(1500L * i)
                        continue
                    }
                    throw e // Break loop for other errors
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

            if (rawResult == null || rawResult.dishName.isEmpty()) {
                return@withContext VisionAiResult.Error("AI không xác định được món ăn này.")
            }

            // --- VALIDATION & CONFIDENCE ENGINE ---
            val finalResult = applyHeuristicValidation(rawResult)
            
            Log.d("VisionAiRepository", "Validation Complete. Final Confidence: ${finalResult.dishConfidence}")

            if (finalResult.dishConfidence < 0.25f) {
                return@withContext VisionAiResult.Error("Hệ thống không tự tin nhận diện món ăn này. Vui lòng chụp rõ hơn.")
            }

            VisionAiResult.Success(finalResult)
        } catch (e: Exception) {
            Log.e("VisionAiRepository", "Pipeline Failure", e)
            val friendlyError = when {
                e.message?.contains("Model AI không khả dụng") == true -> e.message!!
                e.message?.contains("429") == true -> "Bạn đã vượt quá giới hạn lượt dùng thử. Vui lòng đợi 1 phút."
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
