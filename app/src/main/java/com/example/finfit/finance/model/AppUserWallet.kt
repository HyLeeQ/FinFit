package com.example.finfit.finance.model

import com.google.firebase.Timestamp

data class HeldFundItem(val id: String = "", val name: String = "", val amount: Double = 0.0)

// Mỗi khoản trả trước cho nhóm là 1 item riêng biệt
data class GroupPrepaidItem(
        val id: String = "",
        val transactionId: String = "",     // Liên kết tới giao dịch gốc (nếu có)
        val description: String = "",       // VD: "Ăn lẩu nhóm 22/06"
        val totalAmount: Double = 0.0,      // Tổng bill
        val groupOwedAmount: Double = 0.0,  // Số tiền nhóm nợ bạn (chưa thu)
        val collectedAmount: Double = 0.0,  // Số tiền đã thu được
        val participantCount: Int = 1,      // Số người tham gia
        val participants: List<TransactionParticipant> = emptyList(),
        val createdAt: Timestamp = Timestamp.now(),
        val isFullyCollected: Boolean = false // Đã thu hồi hết chưa
)

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
        val groupPrepaidItems: List<GroupPrepaidItem> = emptyList(), // Danh sách từng khoản trả trước cho nhóm
        val heldFunds: List<HeldFundItem> = emptyList(), // Tiền giữ hộ / Quỹ nhóm
        val isTotalBalanceHidden: Boolean = true, // Ẩn biểu đồ tròn phân bổ ở Dashobard
        val autoSaveWeeklySurplus: Boolean = false // Tự động chuyển tiền thừa sang Tiết kiệm
) {
    val totalBalance: Double
        get() = accounts.sumOf { it.amount }

    val totalHeldFunds: Double
        get() = heldFunds.sumOf { it.amount }

    val totalGroupPrepaid: Double
        get() = groupPrepaidItems.filter { !it.isFullyCollected }
                                 .sumOf { it.groupOwedAmount - it.collectedAmount }

    /** Số dư theo nhóm mục đích tài khoản */
    val dailySpendingBalance: Double
        get() = accounts.filter { it.purpose == AccountPurpose.DAILY_SPENDING }.sumOf { it.amount }

    val savingsAccountBalance: Double
        get() = accounts.filter { it.purpose == AccountPurpose.SAVINGS }.sumOf { it.amount }

    val emergencyFundBalance: Double
        get() = accounts.filter { it.purpose == AccountPurpose.EMERGENCY_FUND }.sumOf { it.amount }

    val investmentBalance: Double
        get() = accounts.filter { it.purpose == AccountPurpose.INVESTMENT }.sumOf { it.amount }

    /** Tính tài sản ròng Net Worth: Tổng tài khoản + Tiết kiệm chung + Tiền người khác nợ - Nợ phải trả */
    fun calculateNetWorth(goalsTotal: Double = 0.0, totalDebts: Double = 0.0, totalLoans: Double = 0.0): Double {
        val totalAssets = totalBalance + generalSavings + goalsTotal + totalLoans + totalGroupPrepaid
        return totalAssets - totalDebts
    }
}

