package com.example.finfit.finance.model

// ──────────────────────────────────────────────────────────────
//  Lịch trình chi tiêu tuần (Weekly Schedule)
// ──────────────────────────────────────────────────────────────
data class SpendingScheduleItem(
    val id: String = "",
    val dayOfWeek: Int = 1, // 1: Thứ 2, ..., 7: Chủ Nhật
    val amount: Double = 0.0,
    val category: String = "Ăn uống",
    val note: String = "",
    val isAutoApply: Boolean = false // Tương lai có thể tự động trừ tiền
)
