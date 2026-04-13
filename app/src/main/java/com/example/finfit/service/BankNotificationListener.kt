package com.example.finfit.service

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.model.PaymentMethod
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

class BankNotificationListener : NotificationListenerService() {

    private val firestoreRepository = FirestoreRepository()
    private val authRepository = AuthRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Map package → bankCode
    private val packageToBankCode = mapOf(
        "com.mbmobile"                      to "MB",
        "com.MBBank"                        to "MB",
        "com.vcb.digibank"                  to "VIETCOMBANK",
        "com.vcb.mobile.banking"            to "VIETCOMBANK",
        "com.mservice.momotransfer"         to "MOMO",
        "vn.com.techcombank.bb.app"         to "TECHCOMBANK",
        "com.techcombank.retail.mb"         to "TECHCOMBANK",
        "com.vnpay.bidv"                    to "BIDV",
        "com.bidv.smartbanking"             to "BIDV",
        "com.vnpay.vpbank"                  to "VPBANK",
        "com.vnpay.agribank"                to "AGRIBANK",
        "com.tpb.mb.retail"                 to "TPBANK",
        "vn.tpbank.mobile"                  to "TPBANK",
        "com.zing.zalo"                     to "ZALOPAY",
        "vn.zalopay.merchant"               to "ZALOPAY",
        "com.fpt.viettelpay"                to "VIETTELPAY",
        "com.acb.mobile"                    to "ACB",
        "vn.com.sacombank.mobilebanking"    to "SACOMBANK",
        "com.VCB.Digibank"                  to "VIETCOMBANK",
        "com.shinhanbank.digitaldoor"       to "SHINHAN",
        "vn.ocb.digitalbank"                to "OCB",
        "com.msb.mobilebanking"             to "MSB"
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("BankNoti", "✅ NotificationListenerService đã kết nối")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("BankNoti", "⚠️ Mất kết nối - đang yêu cầu rebind...")
        requestRebind(ComponentName(this, BankNotificationListener::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d("BankNoti", "🛑 Service destroyed, scope hủy")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val bankCode = packageToBankCode[packageName]

        val extras = sbn.notification.extras
        val title  = extras.getString("android.title") ?: ""
        val text   = extras.getString("android.text")  ?: ""
        // Một số NH dùng bigText
        val bigText = extras.getString("android.bigText") ?: ""
        val content = "$title $text $bigText".replace("\n", " ").trim()

        // Nếu không phải app NH đã biết → thử đọc nếu nội dung có từ khóa giao dịch
        if (bankCode == null) {
            // Không xử lý app không rõ nguồn
            return
        }

        Log.d("BankNoti", "📲 [$bankCode] $content")

        val result = parseBankingContent(content) ?: run {
            Log.d("BankNoti", "⚠️ Không parse được nội dung: $content")
            return
        }

        processTransaction(bankCode, result.first, result.second, content, result.third)
    }

    /**
     * Regex rộng bao phủ nhiều định dạng thông báo NH Việt Nam:
     *  - "+1,234,567 VND"  "- 500000 VND"
     *  - "GD: 200000D"  "SD: 150000 VND"
     *  - "so tien: 300.000d"  "amount: 500k"
     *  - "1,500,000VND"  "1.500.000d"
     *  - "TK XXX +/-500000 VND"
     *  - MB: "Tai khoan ... bien dong +200,000 VND"
     *  - MoMo: "Ban da nhan 150.000d tu ..."
     *  - Techcombank: "-350,000 VND TK..."
     */
    private fun parseBankingContent(raw: String): Triple<Double, TransactionType, String?>? {
        // Chuẩn hóa: bỏ dấu phẩy, dấu chấm làm separator (giữ lại chữ, số, dấu +/-)
        val content = raw
            .replace(",", "")
            .replace(Regex("""(\d)\.(\d{3})""")) { it.groupValues[1] + it.groupValues[2] } // 1.500.000 → 1500000

        // ── Các pattern số tiền ──────────────────────────────────────────────
        val patterns = listOf(
            // Pattern 1: ký hiệu [+/-] rõ ràng trước số tiền
            Regex("""([+\-])\s*(\d{4,})(?:\s*(?:VND|vnd|đ|d|D))?"""),
            // Pattern 2: từ khóa trước số tiền (biến động, GD, SD, TK, so tien, amount)
            Regex("""(?:biến động|bien dong|GD|SD|TK|số tiền|so tien|amount|nhan|da nhan|chuyen|chi|thanh toan)\s*[:\s]*([+\-]?\s*\d{4,})(?:\s*(?:VND|vnd|đ|d|D))?""", RegexOption.IGNORE_CASE),
            // Pattern 3: số tiền đứng sau từ "+" hoặc "-" kiểu MB bank
            Regex("""(?:^|[\s(])([+\-]\d{4,})(?:\s*(?:VND|vnd|đ|d|D))?"""),
            // Pattern 4: fallback - số lớn độc lập bất kỳ đơn vị nào
            Regex("""(\d{5,})(?:\s*(?:VND|vnd|đ|d|D))""")
        )

        var rawAmount = 0.0
        var signFromContent: Int? = null  // +1 = income, -1 = expense

        for (pattern in patterns) {
            val match = pattern.find(content) ?: continue
            val groups    = match.groups
            val signStr   = groups[1]?.value?.trim() ?: ""
            // Grup 2 nếu có (pattern có 2 group), nếu không thì dùng grup 1
            val amountStr = (if (groups.size > 2) groups[2] else groups[1])
                ?.value?.replace(Regex("[^0-9]"), "") ?: continue
            val candidate = amountStr.toDoubleOrNull() ?: continue
            if (candidate < 1_000) continue

            rawAmount = candidate
            signFromContent = when {
                signStr == "+" -> +1
                signStr == "-" -> -1
                else           -> null
            }
            break
        }

        if (rawAmount == 0.0) return null

        // ── Phân loại thu/chi ────────────────────────────────────────────────
        val lower = raw.lowercase()

        val incomeKeywords = listOf(
            "+", "nhận", "nhan", "vào", "vao", "tang", "tăng",
            "nap vao", "nạp vào", "cashin", "cash in",
            "hoan tien", "hoàn tiền", "refund", "receive", "credit",
            "so du tang", "số dư tăng", "bien dong +", "biến động +"
        )
        val expenseKeywords = listOf(
            "-", "trừ", "tru", "chi", "thanh toan", "thanh toán",
            "chuyen tien", "chuyển tiền", "rut", "rút",
            "payment", "debit", "so du giam", "số dư giảm",
            "bien dong -", "biến động -", "transfer out"
        )

        val incomeScore  = incomeKeywords.count { lower.contains(it) }
        val expenseScore = expenseKeywords.count { lower.contains(it) }

        // Ưu tiên dấu từ content, nếu không có thì dùng keyword score
        val isIncome = when {
            signFromContent == +1                  -> true
            signFromContent == -1                  -> false
            incomeScore > expenseScore             -> true
            expenseScore > incomeScore             -> false
            else                                   -> false  // default: expense nếu không rõ
        }

        // ── Phát hiện chuyển khoản nội bộ (e.g. nạp MoMo từ ngân hàng) ─────
        val isMomoTransfer   = lower.contains("momo") && (lower.contains("cashin") || lower.contains("nap"))
        val isZaloTransfer   = lower.contains("zalopay") && lower.contains("cashin")
        val isAtmWithdraw    = lower.contains("rut tien") || lower.contains("rút tiền") || lower.contains("atm")

        val type = when {
            isMomoTransfer || isZaloTransfer || isAtmWithdraw -> TransactionType.TRANSFER
            isIncome -> TransactionType.INCOME
            else     -> TransactionType.EXPENSE
        }

        val targetBankCode = when {
            isMomoTransfer  -> "MOMO"
            isZaloTransfer  -> "ZALOPAY"
            isAtmWithdraw   -> "CASH"
            else            -> null
        }

        return Triple(rawAmount, type, targetBankCode)
    }

    private fun processTransaction(
        bankCode: String,
        amount: Double,
        type: TransactionType,
        note: String,
        transferTo: String?
    ) {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            Log.w("BankNoti", "⚠️ Bỏ qua: User chưa đăng nhập")
            return
        }

        serviceScope.launch {
            try {
                val wallet = firestoreRepository.getUserWallet(user.uid)
                if (wallet == null) {
                    Log.w("BankNoti", "⚠️ Không tìm thấy ví cho uid=${user.uid}")
                    return@launch
                }

                // Tìm tài khoản nguồn — ưu tiên match bankCode, fallback về account đầu tiên
                val sourceAccount = wallet.accounts.find { it.bankCode == bankCode }
                    ?: wallet.accounts.firstOrNull()
                    ?: return@launch

                // Tài khoản đích nếu là chuyển khoản
                val destAccount = if (type == TransactionType.TRANSFER && transferTo != null) {
                    wallet.accounts.find { it.bankCode == transferTo }
                } else null

                val updatedAccounts = wallet.accounts.map { acc ->
                    when (acc.id) {
                        sourceAccount.id -> {
                            val newAmt = if (type == TransactionType.INCOME) acc.amount + amount
                                         else acc.amount - amount
                            acc.copy(amount = newAmt)
                        }
                        destAccount?.id  -> acc.copy(amount = acc.amount + amount)
                        else             -> acc
                    }
                }

                firestoreRepository.saveUserWallet(wallet.copy(accounts = updatedAccounts))

                val transaction = FinanceTransaction(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = type,
                    category = when (type) {
                        TransactionType.INCOME   -> "Thu nhập tự động"
                        TransactionType.TRANSFER -> "Chuyển khoản tự động"
                        else                     -> "Chi tiêu tự động"
                    },
                    note = "[Tự động - $bankCode] ${note.take(120)}",
                    paymentMethod = PaymentMethod.BANKING,
                    accountId = sourceAccount.id,
                    toAccountId = destAccount?.id
                )

                firestoreRepository.addTransaction(user.uid, transaction)
                Log.d("BankNoti", "✅ Đã lưu: $type ${amount.toLong()}đ | ${sourceAccount.name} → ${destAccount?.name ?: "–"}")

            } catch (e: Exception) {
                Log.e("BankNoti", "❌ Lỗi xử lý: ${e.message}", e)
            }
        }
    }
}
