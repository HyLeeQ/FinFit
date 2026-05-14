package com.example.finfit.health.data.remote.cloudinary

import android.graphics.Bitmap

/**
 * CloudinaryRepository — High-level interface for image uploads.
 * Keeps business logic separated from the raw SDK implementation.
 */
class CloudinaryRepository(private val cloudinaryService: CloudinaryService) {
    
    /**
     * Uploads a bitmap and returns the secure Cloudinary URL.
     */
    suspend fun uploadImage(bitmap: Bitmap): String? {
        return cloudinaryService.uploadBitmap(bitmap)
    }
}
