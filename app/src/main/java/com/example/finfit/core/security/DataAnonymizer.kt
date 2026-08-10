package com.example.finfit.core.security

import com.example.finfit.finance.model.AppBankAccount
import com.example.finfit.finance.model.DebtLoan

object DataAnonymizer {

    /** Ẩn danh hóa số tài khoản ngân hàng: e.g. "MBBank 123456789" -> "MBBank ****6789" */
    fun anonymizeBankAccount(account: AppBankAccount): String {
        val num = account.accountNumber
        val maskedNum = if (num.length > 4) {
            "****" + num.takeLast(4)
        } else "****"
        return "${account.name} ($maskedNum)"
    }

    /** Ẩn danh hóa tên người trong sổ nợ/cho vay trước khi gửi context cho AI */
    fun anonymizeDebtPersonName(debt: DebtLoan, index: Int): String {
        return "Người quen #$index"
    }

    /** Ẩn danh hóa chuỗi nhạy cảm (Email, Số điện thoại) */
    fun sanitizeRawText(input: String): String {
        val emailRegex = Regex("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+")
        val phoneRegex = Regex("(0|\\+84)[0-9]{9}")
        return input
            .replace(emailRegex, "[EMAIL_ĐÃ_ẨN]")
            .replace(phoneRegex, "[SĐT_ĐÃ_ẨN]")
    }
}
