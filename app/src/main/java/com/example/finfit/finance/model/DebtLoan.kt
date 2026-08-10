package com.example.finfit.finance.model

import com.google.firebase.Timestamp

// ──────────────────────────────────────────────────────────────
//  Quản lý Nợ/Cho vay (Debts & Loans)
// ──────────────────────────────────────────────────────────────
enum class DebtLoanType { DEBT, LOAN }

data class DebtLoan(
    val id: String = "",
    val personName: String = "",
    val personPhone: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0, // Số tiền đã trả một phần
    val type: DebtLoanType = DebtLoanType.DEBT,
    val note: String = "",
    val dueDate: Timestamp? = null,
    val isPaid: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val interestRate: Double = 0.0, // Lãi suất %/năm
    val isInstallment: Boolean = false, // Là khoản trả góp nhiều kỳ
    val totalInstallments: Int = 1, // Tổng số kỳ
    val paidInstallments: Int = 0, // Số kỳ đã trả
    val installmentAmount: Double = 0.0 // Số tiền mỗi kỳ
) {
    /** Số tiền còn lại phải trả/thu */
    val remainingAmount: Double
        get() = if (isPaid) 0.0 else (amount - paidAmount).coerceAtLeast(0.0)

    /** Tính số tiền lãi tích lũy ước tính (nếu có lãi suất) */
    fun calculateAccruedInterest(): Double {
        if (interestRate <= 0.0 || isPaid) return 0.0
        val createdMillis = createdAt.toDate().time
        val nowMillis = System.currentTimeMillis()
        val days = ((nowMillis - createdMillis) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
        return (remainingAmount * (interestRate / 100.0) * (days / 365.0))
    }
}

/** Dữ liệu tổng hợp nợ/cho vay gom theo từng người */
data class PersonDebtSummary(
    val personName: String,
    val totalDebtAmount: Double, // Mình nợ người này
    val totalLoanAmount: Double, // Người này nợ mình
    val netAmount: Double,       // > 0: Người này nợ mình nhiều hơn; < 0: Mình nợ người này nhiều hơn
    val items: List<DebtLoan>
)

