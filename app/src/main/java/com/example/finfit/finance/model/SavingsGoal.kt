package com.example.finfit.finance.model

import com.google.firebase.Timestamp

enum class SavingStrategy {
    FIXED_SCHEDULE,         // Định kỳ cố định (hàng tuần / tháng)
    PERCENT_OF_INCOME,      // Trích % từ mỗi thu nhập phát sinh
    ROUND_UP,               // Làm tròn giao dịch chi tiêu (vd: 43k -> 50k)
    END_OF_MONTH_SURPLUS    // Tích lũy số dư cuối tháng
}

enum class GoalPriority {
    HIGH,
    MEDIUM,
    LOW
}

data class SavingsGoal(
        val id: String = "",
        val goalName: String = "",
        val targetAmount: Double = 0.0,
        val currentAmount: Double = 0.0,
        val targetDate: Timestamp? = null,
        val iconEmoji: String = "🎯",
        val colorHex: Long = 0xFF3B82F6L, // Blue default
        val createdAt: Timestamp = Timestamp.now(),
        val autoSavingAmount: Double = 0.0, // Số tiền tự động nạp mỗi tuần
        val lastAutoSavingAt: Timestamp? = null, // Lần cuối cộng tiền tự động
        val strategy: SavingStrategy = SavingStrategy.FIXED_SCHEDULE,
        val strategyValue: Double = 0.0, // % trích (nếu là PERCENT_OF_INCOME) hoặc bước làm tròn
        val priority: GoalPriority = GoalPriority.MEDIUM,
        val linkedHeldFundId: String? = null // ID liên kết quỹ giữ hộ / nhóm nếu là mục tiêu chung
)

data class Category(
        val id: String = "",
        val name: String = "",
        val type: TransactionType = TransactionType.EXPENSE,
        val isDefault: Boolean = true
)
