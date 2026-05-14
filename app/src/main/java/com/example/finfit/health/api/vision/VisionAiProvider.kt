package com.example.finfit.health.api.vision

import android.graphics.Bitmap

/**
 * An abstraction layer for all Vision AI backend providers.
 * Keeps the application fully decoupled from any specific SDK (Gemini, OpenAI, Claude).
 */
interface VisionAiProvider {
    /**
     * Analyze an image using the provider's multimodal capabilities.
     *
     * @param bitmap The compressed, cropped high-res image.
     * @param prompt The prompt strictly dictating JSON output.
     * @return Raw JSON string response from the provider.
     */
    suspend fun analyzeImage(bitmap: Bitmap, prompt: String): String
}
