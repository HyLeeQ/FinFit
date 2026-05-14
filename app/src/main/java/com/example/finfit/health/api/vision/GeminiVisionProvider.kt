package com.example.finfit.health.api.vision

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.BlockThreshold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of VisionAiProvider using the native Gemini SDK.
 * Handles model initialization, configures JSON response schema rules,
 * and executes the API call over coroutines.
 */
class GeminiVisionProvider(
    private val apiKey: String
) : VisionAiProvider {

    private val safetyConfig = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
    )

    // A robust, prioritized chain of models to guarantee zero "404 Not Found" failures
    // across different Google AI Studio key provisioning tiers and legacy SDK endpoints.
    private val candidateModels = listOf(
        "gemini-2.5-flash",           // Primary provisioned tier model (Direct success, 1 RPM)
        "gemini-2.0-flash",           // Secondary modern tier fallback
        "gemini-1.5-flash"            // Standard highly-compatible legacy string
    )

    override suspend fun analyzeImage(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val inputContent = content {
            image(bitmap)
            text(prompt)
        }

        var lastFallbackException: Exception? = null

        for (modelName in candidateModels) {
            try {
                Log.d("GeminiProvider", "Attempting analysis with model: [$modelName]...")
                val model = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey.trim(),
                    safetySettings = safetyConfig
                )
                val response = model.generateContent(inputContent)
                val text = response.text
                if (!text.isNullOrEmpty()) {
                    Log.d("GeminiProvider", "Analysis successful using model: [$modelName]")
                    return@withContext text
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                Log.w("GeminiProvider", "Model [$modelName] failed: $errorMsg")
                
                // If the error is fatal/global (Key invalid, quota exceeded, blocked location), stop and report immediately!
                when {
                    errorMsg.contains("API_KEY_INVALID") || errorMsg.contains("API key not valid") || errorMsg.contains("key not valid") -> {
                        throw Exception("Lỗi: API Key không hợp lệ. Vui lòng kiểm tra lại dòng VISION_API_KEY trong file local.properties.")
                    }
                    errorMsg.contains("PERMISSION_DENIED") -> {
                        throw Exception("Lỗi: API Key chưa được cấp quyền truy cập dịch vụ Gemini AI.")
                    }
                    errorMsg.contains("USER_LOCATION_NOT_SUPPORTED") -> {
                        throw Exception("Lỗi: Khu vực của bạn hiện chưa được hỗ trợ. Vui lòng bật VPN (Singapore/Mỹ).")
                    }
                    errorMsg.contains("429") || errorMsg.contains("Quota exceeded") || errorMsg.contains("RESOURCE_EXHAUSTED") -> {
                        throw Exception("Lỗi 429: Tài khoản của bạn đã sử dụng hết hạn mức (Quota) miễn phí.")
                    }
                    else -> {
                        // 404 Model Not Found or timeout -> Track and automatically try the next fallback model string!
                        lastFallbackException = e
                    }
                }
            }
        }

        // If all candidate models fail due to availability/404, provide precise failure context
        val fallbackDetail = lastFallbackException?.message ?: "Không xác định"
        val cleanDetail = if (fallbackDetail.length > 100) fallbackDetail.take(100) + "..." else fallbackDetail
        throw Exception("Lỗi: Các phiên bản model AI đều không khả dụng cho API Key này (Chi tiết: $cleanDetail). Vui lòng tạo API Key mới từ Google AI Studio.")
    }
}
