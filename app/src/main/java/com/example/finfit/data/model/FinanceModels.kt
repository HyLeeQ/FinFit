package com.example.finfit.data.model

import com.google.firebase.Timestamp

/**
 * Dữ liệu người dùng và thiết lập ví
 */
data class UserWallet(
    val uid: String = "",
    val savingsAmount: Double = 0.0,
    val disposableAmount: Double = 0.0,
    val isSavingsHidden: Boolean = false,
    val isDisposableHidden: Boolean = false,
    val totalBalance: Double = savingsAmount + disposableAmount
)

/**
 * Dữ liệu giao dịch (Thu nhập, Chi tiêu, hoặc Chuyển tiền)
 */
data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String = "", // Ví dụ: "Ăn uống", "Lương", "Tiết kiệm"
    val note: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val timestamp: Timestamp = Timestamp.now(),
    val isFromOCR: Boolean = false,
    val imageUrl: String? = null, // Đường dẫn ảnh hóa đơn từ Firebase Storage
    val linkedGoalId: String? = null // Liên kết với ID của mục tiêu tiết kiệm (nếu có)
)

/**
 * Mục tiêu tiết kiệm
 */
data class SavingsGoal(
    val id: String = "",
    val goalName: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * Phân loại chi tiêu/thu nhập
 */
data class Category(
    val id: String = "",
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val isDefault: Boolean = true // True nếu là mặc định, False nếu người dùng tự tạo
)

enum class TransactionType {
    EXPENSE,  // Chi tiêu
    INCOME,   // Thu nhập
    TRANSFER  // Chuyển tiền (Ví dụ: Từ tiền mặt sang tiết kiệm)
}

enum class PaymentMethod {
    CASH,    // Tiền mặt
    BANKING  // App ngân hàng/Ví điện tử
}

