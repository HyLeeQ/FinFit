package com.example.finfit.finance.model

import com.google.firebase.Timestamp

// ──────────────────────────────────────────────────────────────
//  Thông tin ngân hàng / ví điện tử
// ──────────────────────────────────────────────────────────────
data class BankInfo(
        val code: String,
        val displayName: String,
        val emoji: String,
        val primaryColorHex: Long // dùng để vẽ gradient card
)

/** Danh sách ngân hàng và ví điện tử Việt Nam phổ biến */
val SUPPORTED_BANKS =
        listOf(
                BankInfo("CASH", "Tiền mặt", "💵", 0xFF059669L),
                BankInfo("MB", "MB Bank", "🏦", 0xFF0066CCL),
                BankInfo("MOMO", "MoMo", "💜", 0xFFAE1F7EL),
                BankInfo("ZALOPAY", "ZaloPay", "💙", 0xFF0068FFL),
                BankInfo("TECHCOMBANK", "Techcombank", "❤️", 0xFFE31837L),
                BankInfo("VIETCOMBANK", "Vietcombank", "🟢", 0xFF007A33L),
                BankInfo("BIDV", "BIDV", "🏦", 0xFF003E7EL),
                BankInfo("VPBANK", "VPBank", "💚", 0xFF00A651L),
                BankInfo("ACB", "ACB", "🏦", 0xFF005B9AL),
                BankInfo("SACOMBANK", "Sacombank", "🏦", 0xFF009B4DL),
                BankInfo("VIETINBANK", "VietinBank", "🏦", 0xFF006838L),
                BankInfo("AGRIBANK", "Agribank", "🌿", 0xFF007A33L),
                BankInfo("TPBANK", "TPBank", "🏦", 0xFF6B21A8L),
                BankInfo("MSBANK", "MSB", "🏦", 0xFFDC2626L),
                BankInfo("SHINHAN", "Shinhan Bank", "🏦", 0xFF1D4ED8L),
                BankInfo("OTHER", "Khác", "💳", 0xFF6B7280L),
        )

// ──────────────────────────────────────────────────────────────
//  Tài khoản ngân hàng / ví
// ──────────────────────────────────────────────────────────────
data class AppBankAccount(
        val id: String = "",
        val bankCode: String = "OTHER", // mã nhận biết ngân hàng
        val name: String = "", // tên hiển thị do người dùng đặt
        val amount: Double = 0.0,
        val colorIndex: Int = 0, // 0-5: bảng màu gradient card
        val isHidden: Boolean = true
) {
    /** Tổng số dư (tính nhanh ở UI) */
    val displayName: String
        get() =
                name.ifBlank {
                    SUPPORTED_BANKS.find { it.code == bankCode }?.displayName ?: "Tài khoản"
                }
}

data class HeldFundItem(val id: String = "", val name: String = "", val amount: Double = 0.0)

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
        val accounts: List<AppBankAccount> = emptyList(),
        val generalSavings: Double = 0.0, // Tiết kiệm không mục đích (Dự phòng)
        val groupPrepaidAmount: Double = 0.0, // Số tiền đã trả trước cho nhóm (người khác nợ mình)
        val heldFunds: List<HeldFundItem> = emptyList(), // Tiền giữ hộ / Quỹ nhóm
        val isTotalBalanceHidden: Boolean = true, // Ẩn biểu đồ tròn phân bổ ở Dashobard
        val autoSaveWeeklySurplus: Boolean = false // Tự động chuyển tiền thừa sang Tiết kiệm
) {
    val totalBalance: Double
        get() = accounts.sumOf { it.amount }

    val totalHeldFunds: Double
        get() = heldFunds.sumOf { it.amount }
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
        val linkedGoalId: String? = null,
        val accountId: String? = null, // ID của tài khoản thực hiện (from)
        val toAccountId: String? = null, // ID của tài khoản nhận (cho chuyển khoản)
        val isGroupPrepayment: Boolean = false, // Đánh dấu là trả trước cho nhóm
        val personalAmount: Double = 0.0, // Phần tiền cá nhân chịu (trong giao dịch chia sẻ)
        val groupAmount: Double = 0.0,    // Phần tiền người khác chịu (trả trước hộ)
        val participantCount: Int = 1     // Số người tham gia chia tiền
)

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
        val lastAutoSavingAt: Timestamp? = null // Lần cuối cộng tiền tự động
)

data class Category(
        val id: String = "",
        val name: String = "",
        val type: TransactionType = TransactionType.EXPENSE,
        val isDefault: Boolean = true
)

// ──────────────────────────────────────────────────────────────
//  Hạn mức chi tiêu (Budgets)
// ──────────────────────────────────────────────────────────────
enum class BudgetPeriod {
    WEEKLY,
    MONTHLY
}

// Thêm field categoryId hoặc group để dễ quản lý budget hơn nếu cần
data class FinanceBudget(
        val id: String = "",
        val amount: Double = 0.0,
        val period: BudgetPeriod = BudgetPeriod.MONTHLY,
        val category: String = "Tất cả", // "Tất cả" hoặc tên hạng mục cụ thể
        val startDate: Timestamp = Timestamp.now()
)

// ──────────────────────────────────────────────────────────────
//  Quản lý Nợ/Cho vay (Debts & Loans)
// ──────────────────────────────────────────────────────────────
enum class DebtLoanType { DEBT, LOAN }

data class DebtLoan(
    val id: String = "",
    val personName: String = "",
    val amount: Double = 0.0,
    val type: DebtLoanType = DebtLoanType.DEBT,
    val note: String = "",
    val dueDate: Timestamp? = null,
    val isPaid: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)

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

// ──────────────────────────────────────────────────────────────
//  Thói quen & Lịch trình thông minh (AI Persona)
// ──────────────────────────────────────────────────────────────
data class RoutineSchedule(
    val startDay: Int = 1, // 1: Thứ 2
    val endDay: Int = 3,   // 3: Thứ 4
    val location: String = "Trọ", // "Trọ" hoặc "Nhà"
    val note: String = ""
)

data class UserHabit(
    val minMealCost: Double = 0.0,
    val maxMealCost: Double = 0.0,
    val routineSchedules: List<RoutineSchedule> = emptyList(),
    val fixedCosts: List<SpendingScheduleItem> = emptyList(),
    val lastProactiveWeek: String = "", // Định dạng "yyyy-ww" để kiểm tra đã hỏi trong tuần chưa
    val generalNotes: String = "" // "ở nhà = không tốn tiền ăn", v.v.
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
