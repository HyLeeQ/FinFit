package com.example.finfit.finance.model

import com.google.firebase.Timestamp

// ──────────────────────────────────────────────────────────────
//  Thông tin ngân hàng / ví điện tử
// ──────────────────────────────────────────────────────────────
data class BankInfo(
    val code: String,
    val displayName: String,
    val emoji: String,
    val primaryColorHex: Long   // dùng để vẽ gradient card
)

/** Danh sách ngân hàng và ví điện tử Việt Nam phổ biến */
val SUPPORTED_BANKS = listOf(
    BankInfo("CASH",        "Tiền mặt",       "💵", 0xFF059669L),
    BankInfo("MB",          "MB Bank",         "🏦", 0xFF0066CCL),
    BankInfo("MOMO",        "MoMo",            "💜", 0xFFAE1F7EL),
    BankInfo("ZALOPAY",     "ZaloPay",         "💙", 0xFF0068FFL),
    BankInfo("TECHCOMBANK", "Techcombank",     "❤️", 0xFFE31837L),
    BankInfo("VIETCOMBANK", "Vietcombank",     "🟢", 0xFF007A33L),
    BankInfo("BIDV",        "BIDV",            "🏦", 0xFF003E7EL),
    BankInfo("VPBANK",      "VPBank",          "💚", 0xFF00A651L),
    BankInfo("ACB",         "ACB",             "🏦", 0xFF005B9AL),
    BankInfo("SACOMBANK",   "Sacombank",       "🏦", 0xFF009B4DL),
    BankInfo("VIETINBANK",  "VietinBank",      "🏦", 0xFF006838L),
    BankInfo("AGRIBANK",    "Agribank",        "🌿", 0xFF007A33L),
    BankInfo("TPBANK",      "TPBank",          "🏦", 0xFF6B21A8L),
    BankInfo("MSBANK",      "MSB",             "🏦", 0xFFDC2626L),
    BankInfo("SHINHAN",     "Shinhan Bank",    "🏦", 0xFF1D4ED8L),
    BankInfo("OTHER",       "Khác",            "💳", 0xFF6B7280L),
)

// ──────────────────────────────────────────────────────────────
//  Tài khoản ngân hàng / ví
// ──────────────────────────────────────────────────────────────
data class AppBankAccount(
    val id: String = "",
    val bankCode: String = "OTHER",    // mã nhận biết ngân hàng
    val name: String = "",             // tên hiển thị do người dùng đặt
    val amount: Double = 0.0,
    val colorIndex: Int = 0,           // 0-5: bảng màu gradient card
    val isHidden: Boolean = true
) {
    /** Tổng số dư (tính nhanh ở UI) */
    val displayName: String get() = name.ifBlank {
        SUPPORTED_BANKS.find { it.code == bankCode }?.displayName ?: "Tài khoản"
    }
}

// ──────────────────────────────────────────────────────────────
//  Ví tổng hợp
// ──────────────────────────────────────────────────────────────
data class AppUserWallet(
    val uid: String = "",
    // Thông tin cũ → giữ để không mất data Firestore
    val savingsAmount: Double = 0.0,
    val disposableAmount: Double = 0.0,
    val isSavingsHidden: Boolean = true,
    val isDisposableHidden: Boolean = true,
    val card1Name: String = "THẺ CHÍNH",
    val card1Type: String = "Thẻ ngân hàng",
    val card1Color: Int = 0,
    val card2Name: String = "TIỀN MẶT",
    val card2Type: String = "Tiền mặt",
    val card2Color: Int = 1,
    // === Mới: danh sách tài khoản đa năng ===
    val accounts: List<AppBankAccount> = emptyList()
) {
    val totalBalance: Double
        get() = accounts.sumOf { it.amount }
}

// ──────────────────────────────────────────────────────────────
//  Giao dịch
// ──────────────────────────────────────────────────────────────
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
    val linkedGoalId: String? = null
)

data class SavingsGoal(
    val id: String = "",
    val goalName: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now()
)

data class Category(
    val id: String = "",
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val isDefault: Boolean = true
)

enum class TransactionType { EXPENSE, INCOME, TRANSFER }
enum class PaymentMethod { CASH, BANKING }
