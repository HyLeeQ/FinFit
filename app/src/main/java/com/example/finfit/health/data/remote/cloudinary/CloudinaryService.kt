package com.example.finfit.health.data.remote.cloudinary

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.finfit.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.coroutines.resume

/**
 * CloudinaryService — Handles raw SDK interaction with Cloudinary.
 * Implements image compression and secure upload.
 */
class CloudinaryService(private val context: Context) {

    init {
        initialize(context)
    }

    companion object {
        private var isInitialized = false

        fun initialize(context: Context) {
            if (isInitialized) return
            try {
                val config = mapOf(
                    "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                    "secure" to true
                )
                MediaManager.init(context, config)
                isInitialized = true
            } catch (e: Exception) {
                Log.e("CloudinaryService", "Initialization error: ${e.message}")
            }
        }
    }

    suspend fun uploadBitmap(bitmap: Bitmap): String? = suspendCancellableCoroutine { continuation ->
        val file = createTempFileFromBitmap(bitmap) ?: run {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        MediaManager.get().upload(file.absolutePath)
            .option("unsigned", true)
            .option("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    Log.d("CloudinaryService", "Upload success: $url")
                    file.delete()
                    if (continuation.isActive) continuation.resume(url)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("CloudinaryService", "Upload error: ${error.description}")
                    file.delete()
                    if (continuation.isActive) continuation.resume(null)
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    file.delete()
                    if (continuation.isActive) continuation.resume(null)
                }
            }).dispatch()
    }

    private fun createTempFileFromBitmap(bitmap: Bitmap): File? {
        return try {
            val file = File(context.cacheDir, "meal_upload_${UUID.randomUUID()}.jpg")
            val bos = ByteArrayOutputStream()
            // Compress bitmap to 80% quality to save bandwidth while preserving detail
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
            val bitmapData = bos.toByteArray()

            val fos = FileOutputStream(file)
            fos.write(bitmapData)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            Log.e("CloudinaryService", "Temp file error: ${e.message}")
            null
        }
    }
}
