package com.example.finfit.finance.model

import com.google.firebase.Timestamp

// ──────────────────────────────────────────────────────────────
//  Hạn mức chi tiêu (Budgets)
// ──────────────────────────────────────────────────────────────
enum class BudgetPeriod {
    WEEKLY,
    MONTHLY
}

data class FinanceBudget(
        val id: String = "",
        val amount: Double = 0.0,
        val period: BudgetPeriod = BudgetPeriod.MONTHLY,
        val category: String = "Tất cả", // "Tất cả" hoặc tên hạng mục cụ thể
        val startDate: Timestamp = Timestamp.now(),
        val isRollover: Boolean = false, // Tự động cộng dồn số dư sang tháng sau
        val rolloverAmount: Double = 0.0, // Số dư cộng dồn từ kỳ trước
        val isEnvelope: Boolean = false, // Chế độ phong bì cứng
        val envelopeAllocated: Double = 0.0 // Số tiền đã phân bổ cho phong bì
)
