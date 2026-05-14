package com.example.finfit.health.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer


/**
 * Extension to convert CameraX ImageProxy to Bitmap safely.
 */
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val uBuffer = planes[1].buffer // U
    val vBuffer = planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    //U and V are swapped
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = android.graphics.YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

object BitmapUtils {

    /**
     * Reverses the YOLO Letterbox padding and scaling to find the exact coordinates
     * on the original bitmap, and then crops that region.
     */
    fun cropFromBoundingBox(
        originalBitmap: Bitmap,
        boundingBox: RectF,
        modelInputSize: Int = 640
    ): Bitmap? {
        try {
            val origW = originalBitmap.width.toFloat()
            val origH = originalBitmap.height.toFloat()

            // 1. Calculate the same scale and padding that YOLO used during preprocessing
            val scale = minOf(modelInputSize / origW, modelInputSize / origH)
            val scaledW = Math.round(origW * scale).toFloat()
            val scaledH = Math.round(origH * scale).toFloat()

            val padX = (modelInputSize - scaledW) / 2f
            val padY = (modelInputSize - scaledH) / 2f

            // 2. Reverse the transform for left/right/top/bottom
            val leftOrig = ((boundingBox.left - padX) / scale).toInt()
            val topOrig = ((boundingBox.top - padY) / scale).toInt()
            val rightOrig = ((boundingBox.right - padX) / scale).toInt()
            val bottomOrig = ((boundingBox.bottom - padY) / scale).toInt()

            // 3. Clamp safely to bitmap bounds
            val safeLeft = leftOrig.coerceIn(0, originalBitmap.width - 1)
            val safeTop = topOrig.coerceIn(0, originalBitmap.height - 1)
            val safeRight = rightOrig.coerceIn(safeLeft + 1, originalBitmap.width)
            val safeBottom = bottomOrig.coerceIn(safeTop + 1, originalBitmap.height)

            val cropWidth = safeRight - safeLeft
            val cropHeight = safeBottom - safeTop

            if (cropWidth <= 0 || cropHeight <= 0) {
                Log.e("BitmapUtils", "Invalid crop dimensions: ${cropWidth}x${cropHeight}")
                return null
            }

            Log.d("BitmapUtils", "Cropped $cropWidth x $cropHeight from Original ${originalBitmap.width}x${originalBitmap.height}")
            return Bitmap.createBitmap(originalBitmap, safeLeft, safeTop, cropWidth, cropHeight)
        } catch (e: Exception) {
            Log.e("BitmapUtils", "Failed to crop bitmap", e)
            return null
        }
    }
}
