package com.example.finfit.finance.model

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
