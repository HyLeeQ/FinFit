package com.example.finfit.finance.model

// ──────────────────────────────────────────────────────────────
//  Tài khoản ngân hàng / ví
// ──────────────────────────────────────────────────────────────
enum class AccountPurpose(val displayName: String, val iconEmoji: String) {
    DAILY_SPENDING("Chi tiêu hàng ngày", "💳"),
    SAVINGS("Tài khoản tiết kiệm", "💰"),
    EMERGENCY_FUND("Dự phòng khẩn cấp", "🛡️"),
    INVESTMENT("Đầu tư / Khác", "📈")
}

data class AppBankAccount(
        val id: String = "",
        val bankCode: String = "OTHER", // mã nhận biết ngân hàng
        val name: String = "", // tên hiển thị do người dùng đặt
        val amount: Double = 0.0,
        val colorIndex: Int = 0, // 0-5: bảng màu gradient card
        val isHidden: Boolean = true,
        val purpose: AccountPurpose = AccountPurpose.DAILY_SPENDING, // Phân loại tài khoản theo mục đích
        val lowBalanceThreshold: Double = 0.0, // Ngưỡng cảnh báo số dư thấp (0 = tắt)
        val accountNumber: String = "" // Số tài khoản (để tạo VietQR)
) {
    /** Tổng số dư (tính nhanh ở UI) */
    val displayName: String
        get() =
                name.ifBlank {
                    SUPPORTED_BANKS.find { it.code == bankCode }?.displayName ?: "Tài khoản"
                }

    /** Kiểm tra xem số dư có thấp hơn ngưỡng cảnh báo không */
    val isLowBalance: Boolean
        get() = lowBalanceThreshold > 0 && amount <= lowBalanceThreshold
}

