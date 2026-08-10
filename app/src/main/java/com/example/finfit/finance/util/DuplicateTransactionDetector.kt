package com.example.finfit.finance.util

import com.example.finfit.finance.model.FinanceTransaction
import java.util.Calendar
import kotlin.math.abs

data class DuplicateWarning(
    val isDuplicateCandidate: Boolean,
    val matchedTransaction: FinanceTransaction? = null,
    val warningMessage: String = ""
)

object DuplicateTransactionDetector {

    /**
     * Kiểm tra xem giao dịch mới có khả năng bị trùng lặp với giao dịch đã có hay không
     * (thường xảy ra khi cùng 1 giao dịch vừa được đọc qua SMS, vừa chụp Bill OCR hoặc vừa nhập tay).
     */
    fun checkForDuplicate(
        amount: Double,
        timestampMillis: Long = System.currentTimeMillis(),
        transactions: List<FinanceTransaction>,
        currentTxId: String = ""
    ): DuplicateWarning {
        if (amount <= 0.0 || transactions.isEmpty()) {
            return DuplicateWarning(false)
        }

        val thirtyMinutesMillis = 30 * 60 * 1000L
        val sameDayWindowMillis = 24 * 60 * 60 * 1000L

        // 1. Kiểm tra trùng số tiền và thời gian trong vòng 30 phút (độ tin cậy trùng lặp rất cao)
        val highConfidenceMatch = transactions.find { tx ->
            tx.id != currentTxId &&
            abs(tx.amount - amount) < 1.0 &&
            abs(tx.timestamp.toDate().time - timestampMillis) <= thirtyMinutesMillis
        }

        if (highConfidenceMatch != null) {
            val timeStr = java.text.SimpleDateFormat("HH:mm dd/MM", java.util.Locale.getDefault())
                .format(highConfidenceMatch.timestamp.toDate())
            return DuplicateWarning(
                isDuplicateCandidate = true,
                matchedTransaction = highConfidenceMatch,
                warningMessage = "⚠️ Phát hiện giao dịch tương tự: ${formatAmount(amount)} (${highConfidenceMatch.category} lúc $timeStr). Hãy kiểm tra để tránh nhập trùng!"
            )
        }

        // 2. Kiểm tra trùng số tiền trong cùng 1 ngày
        val sameDayMatch = transactions.find { tx ->
            tx.id != currentTxId &&
            abs(tx.amount - amount) < 1.0 &&
            abs(tx.timestamp.toDate().time - timestampMillis) <= sameDayWindowMillis
        }

        if (sameDayMatch != null) {
            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(sameDayMatch.timestamp.toDate())
            return DuplicateWarning(
                isDuplicateCandidate = true,
                matchedTransaction = sameDayMatch,
                warningMessage = "💡 Bạn đã có 1 giao dịch ${formatAmount(amount)} (${sameDayMatch.category} lúc $timeStr) trong hôm nay."
            )
        }

        return DuplicateWarning(false)
    }

    private fun formatAmount(amount: Double): String {
        return String.format("%,d đ", amount.toLong()).replace(',', '.')
    }
}
