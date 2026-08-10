package com.example.finfit.finance.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object VietQrGenerator {

    /**
     * Map mã ngân hàng trong FinFit sang mã chuẩn VietQR (NAPAS)
     */
    private val BANK_CODE_TO_VIETQR = mapOf(
        "MB" to "MB",
        "TECHCOMBANK" to "TCB",
        "VIETCOMBANK" to "VCB",
        "BIDV" to "BIDV",
        "VPBANK" to "VPB",
        "ACB" to "ACB",
        "SACOMBANK" to "STB",
        "VIETINBANK" to "CTG",
        "AGRIBANK" to "VBA",
        "TPBANK" to "TPB",
        "MSBANK" to "MSB",
        "SHINHAN" to "SHBVN",
        "MOMO" to "MOMO",
        "ZALOPAY" to "ZALOPAY"
    )

    /**
     * Sinh URL ảnh mã QR chuyển khoản VietQR nhanh
     */
    fun generateQrUrl(
        bankCode: String,
        accountNumber: String,
        amount: Double = 0.0,
        description: String = "",
        accountName: String = ""
    ): String {
        val vietQrBank = BANK_CODE_TO_VIETQR[bankCode.uppercase()] ?: bankCode.uppercase()
        val cleanAcc = accountNumber.trim().replace(" ", "")
        if (cleanAcc.isBlank()) return ""

        val encodedDesc = try {
            URLEncoder.encode(description, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) { "" }

        val encodedName = try {
            URLEncoder.encode(accountName, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) { "" }

        val amountInt = amount.toLong()
        val queryParams = mutableListOf<String>()
        if (amountInt > 0) queryParams.add("amount=$amountInt")
        if (encodedDesc.isNotBlank()) queryParams.add("addInfo=$encodedDesc")
        if (encodedName.isNotBlank()) queryParams.add("accountName=$encodedName")

        val queryString = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return "https://img.vietqr.io/image/$vietQrBank-$cleanAcc-compact2.png$queryString"
    }

    /**
     * Tạo tin nhắn văn bản chuyển khoản nhanh để sao chép / gửi qua Zalo / SMS
     */
    fun generateTransferMessage(
        bankDisplayName: String,
        accountNumber: String,
        accountName: String,
        amount: Double,
        description: String
    ): String {
        val amountStr = String.format("%,d", amount.toLong()).replace(',', '.') + " đ"
        return """
            🔔 THÔNG TIN THANH TOÁN / CHUYỂN KHOẢN:
            🏦 Ngân hàng: $bankDisplayName
            💳 Số tài khoản: $accountNumber
            👤 Chủ tài khoản: ${accountName.ifBlank { "Chủ tài khoản" }}
            💰 Số tiền: $amountStr
            📝 Nội dung: $description
            (Bạn có thể quét mã VietQR để thanh toán nhanh)
        """.trimIndent()
    }
}
