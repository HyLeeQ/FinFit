package com.example.finfit.finance.model

import com.google.firebase.Timestamp

// ──────────────────────────────────────────────────────────────
//  Giao dịch
// ──────────────────────────────────────────────────────────────
data class TransactionParticipant(
        val name: String = "",
        val shareAmount: Double = 0.0,
        val paidAmount: Double = 0.0,
        val isPaid: Boolean = false
)

data class FinanceTransaction(
        val id: String = "",
        val amount: Double = 0.0,
        val type: TransactionType = TransactionType.EXPENSE,
        val category: String = "",
        val note: String = "",
        val paymentMethod: PaymentMethod = PaymentMethod.CASH,
        val timestamp: Timestamp = Timestamp.now(),
        val isFromOCR: Boolean = false,
        val imageUrl: String? = null,
        val linkedGoalId: String? = null,
        val accountId: String? = null, // ID của tài khoản thực hiện (from)
        val toAccountId: String? = null, // ID của tài khoản nhận (cho chuyển khoản)
        val isGroupPrepayment: Boolean = false, // Đánh dấu là trả trước cho nhóm
        val personalAmount: Double = 0.0, // Phần tiền cá nhân chịu (trong giao dịch chia sẻ)
        val groupAmount: Double = 0.0,    // Phần tiền người khác chịu (trả trước hộ)
        val participantCount: Int = 1,    // Số người tham gia chia tiền
        val participants: List<TransactionParticipant> = emptyList(), // Chi tiết từng người tham gia
        val linkedMealId: String? = null, // Liên kết tới bữa ăn Dinh dưỡng
        val isHomeCooked: Boolean = false, // Tự nấu ăn hay ăn ngoài
        val calorieEstimate: Int? = null   // Ước tính calo của bữa ăn này
)

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    GROUP_PREPAYMENT
}

enum class PaymentMethod {
    CASH,
    BANKING
}
