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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Concrete implementation of VisionAiProvider using the native Gemini SDK.
 *
 * Supports **automatic API key rotation**: when the active key hits a 429/quota
 * exhaustion error, the provider transparently switches to the next key in the
 * pool and retries the request — maximising uptime within the free tier.
 *
 * @param apiKeys  Ordered list of Gemini API keys. The first non-empty key is
 *                 used as primary; the rest serve as hot-standby.
 */
class GeminiVisionProvider(
    private val apiKeys: List<String>
) : VisionAiProvider {

    // Tracks which key in the pool is currently active (shared across calls).
    private val currentKeyIndex = AtomicInteger(0)

    private val safetyConfig = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
    )

    private val candidateModels = listOf(
        "gemini-2.5-flash",   // Primary high-speed model
        "gemini-2.0-flash",   // Stable fallback
        "gemini-1.5-flash"    // Legacy fallback
    )

    /** Returns the filtered (non-empty) list of valid keys. */
    private val validKeys: List<String>
        get() = apiKeys.filter { it.isNotBlank() }

    override suspend fun analyzeImage(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val keys = validKeys
        if (keys.isEmpty()) {
            throw Exception("Lỗi: Chưa cấu hình API Key. Vui lòng thêm VISION_API_KEY vào file local.properties.")
        }

        val inputContent = content {
            image(bitmap)
            text(prompt)
        }

        // We try every available key. For each key, we try every candidate model.
        var lastException: Exception? = null
        val startIndex = currentKeyIndex.get().coerceIn(0, keys.size - 1)

        for (keyOffset in keys.indices) {
            val keyIndex = (startIndex + keyOffset) % keys.size
            val apiKey = keys[keyIndex]
            Log.d("GeminiProvider", "Using API key slot #${keyIndex + 1} of ${keys.size}")

            for (modelName in candidateModels) {
                try {
                    Log.d("GeminiProvider", "  Attempting model [$modelName] with key #${keyIndex + 1}...")
                    val model = GenerativeModel(
                        modelName = modelName,
                        apiKey = apiKey.trim(),
                        safetySettings = safetyConfig
                    )
                    val response = model.generateContent(inputContent)
                    val text = response.text
                    if (!text.isNullOrEmpty()) {
                        // Success — remember this key index for next call
                        currentKeyIndex.set(keyIndex)
                        Log.d("GeminiProvider", "  ✅ Success: model=$modelName, key=#${keyIndex + 1}")
                        return@withContext text
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    Log.w("GeminiProvider", "  ❌ key=#${keyIndex + 1}, model=$modelName → $msg")
                    lastException = e

                    // Fatal errors: stop immediately — no key will fix these
                    when {
                        msg.contains("API_KEY_INVALID") ||
                        msg.contains("API key not valid") ||
                        msg.contains("key not valid") -> {
                            throw Exception("Lỗi: API Key #${keyIndex + 1} không hợp lệ. Kiểm tra lại VISION_API_KEY_${keyIndex + 1} trong local.properties.")
                        }
                        msg.contains("PERMISSION_DENIED") -> {
                            throw Exception("Lỗi: API Key #${keyIndex + 1} chưa được cấp quyền truy cập Gemini AI.")
                        }
                        msg.contains("USER_LOCATION_NOT_SUPPORTED") -> {
                            throw Exception("Lỗi: Khu vực của bạn chưa được hỗ trợ. Vui lòng bật VPN (Singapore/Mỹ).")
                        }
                    }

                    // 429 / Quota exhausted → break out of model loop, try next key
                    if (msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") ||
                        msg.contains("Quota") || msg.contains("quota") ||
                        msg.contains("limit")) {
                        Log.w("GeminiProvider", "  ⚠️ Quota exhausted on key #${keyIndex + 1}. Rotating to next key...")
                        break  // Skip remaining models for this key; go to next key
                    }
                    // For other errors (503, timeout, etc.) try the next model with same key
                }
            }
        }

        // All keys exhausted — compose an informative error
        val errMsg = lastException?.message ?: "Không xác định"
        val cleanErr = if (errMsg.length > 200) errMsg.take(200) + "..." else errMsg

        if (cleanErr.contains("429") || cleanErr.contains("RESOURCE_EXHAUSTED") ||
            cleanErr.contains("Quota") || cleanErr.contains("quota")) {
            throw Exception(
                "Hết hạn mức AI Cloud. Chi tiết: Lỗi 429: Tất cả ${keys.size} API key đã hết quota trong ngày hôm nay. " +
                "Vui lòng thêm key mới tại https://aistudio.google.com/apikey hoặc thử lại sau 00:00 UTC. Chi tiết: $cleanErr"
            )
        }

        throw Exception(
            "Lỗi: Không thể kết nối với Gemini AI sau khi thử tất cả ${keys.size} key và ${candidateModels.size} model. " +
            "Chi tiết: $cleanErr"
        )
    }
}
