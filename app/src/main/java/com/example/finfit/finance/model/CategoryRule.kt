package com.example.finfit.finance.model

/**
 * Luật phân loại tự động học từ thói quen người dùng
 * Ví dụ: "Highlands" -> "Ăn uống", "Grab" -> "Di chuyển"
 */
data class CategoryRule(
    val id: String = "",
    val keyword: String = "",
    val category: String = "",
    val confidenceCount: Int = 1 // Số lần người dùng đã xác nhận hoặc sửa
)
