package com.example.finfit.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.finfit.finance.model.Transaction
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.model.UserWallet
import com.example.finfit.finance.model.PaymentMethod
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class BankNotificationListener : NotificationListenerService() {

    private val firestoreRepository = FirestoreRepository()
    private val authRepository = AuthRepository()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Map các package thông dụng ở VN sang BankCode (đã định nghĩa trong FinanceModels.kt)
    private val packageToBankCode = mapOf(
        "com.mbmobile" to "MB",
        "com.vcb.digibank" to "VIETCOMBANK",
        "com.mservice.momotransfer" to "MOMO",
        "vn.com.techcombank.bb.app" to "TECHCOMBANK",
        "com.techcombank.retail.mb" to "TECHCOMBANK",
        "com.vnpay.bidv" to "BIDV",
        "com.vnpay.vpbank" to "VPBANK",
        "com.tpb.mb.retail" to "TPBANK",
        "com.zing.zalo" to "ZALOPAY"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val bankCode = packageToBankCode[packageName] ?: return
        
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""
        val content = "$title $text"

        Log.d("BankNoti", "Package: $packageName | BankCode: $bankCode | Content: $content")

        // Parse số tiền và dấu
        val result = parseBankingContent(content) ?: return
        val amount = result.first
        val type = result.second

        processTransaction(bankCode, amount, type, content)
    }

    private fun parseBankingContent(content: String): Pair<Double, TransactionType>? {
        // Regex đơn giản để tìm số tiền (ví dụ: +50,000, -20.000, 100,000 VND)
        // Lưu ý: Regex này cần tinh chỉnh thêm cho từng ngân hàng
        val regex = """([+-])?\s?([\d,.]+)\s?(VND|đ|đ)?""".toRegex(RegexOption.IGNORE_CASE)
        val matches = regex.findAll(content)

        for (match in matches) {
            val sign = match.groups[1]?.value
            val valueStr = match.groups[2]?.value?.replace(",", "")?.replace(".", "")
            val value = valueStr?.toDoubleOrNull() ?: continue

            if (value < 1000) continue // Bỏ qua các tin nhắn linh tinh or phí quá nhỏ?

            val isIncome = sign == "+" || content.contains("biến động số dư +", ignoreCase = true) || content.contains("nhận", ignoreCase = true)
            val isExpense = sign == "-" || content.contains("trừ", ignoreCase = true) || content.contains("thanh toán", ignoreCase = true)

            return if (isIncome) {
                Pair(value, TransactionType.INCOME)
            } else if (isExpense || sign == "-") {
                Pair(value, TransactionType.EXPENSE)
            } else {
                // Mặc định nếu không rõ dấu nhưng có số thì tạm coi là Expense? 
                // Có thể cần logic phức tạp hơn ở đây theo từng bank
                Pair(value, TransactionType.EXPENSE)
            }
        }
        return null
    }

    private fun processTransaction(bankCode: String, amount: Double, type: TransactionType, note: String) {
        val user = authRepository.getCurrentUser() ?: return
        
        serviceScope.launch {
            try {
                val wallet = firestoreRepository.getUserWallet(user.uid) ?: return@launch
                
                // Tìm tài khoản phù hợp với bankCode
                val targetAccount = wallet.accounts.find { it.bankCode == bankCode } ?: return@launch
                
                val updatedAccounts = wallet.accounts.map {
                    if (it.id == targetAccount.id) {
                        val newAmount = if (type == TransactionType.INCOME) it.amount + amount else it.amount - amount
                        it.copy(amount = newAmount)
                    } else it
                }
                
                val updatedWallet = wallet.copy(accounts = updatedAccounts)
                
                // Lưu vào Firestore
                firestoreRepository.saveUserWallet(updatedWallet)
                
                // Tạo và lưu bản ghi Transaction
                val newTx = Transaction(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = type,
                    category = if (type == TransactionType.INCOME) "Thu nhập khác" else "Chi tiêu khác",
                    note = "[Tự động] $note",
                    paymentMethod = PaymentMethod.BANKING
                )
                firestoreRepository.addTransaction(user.uid, newTx)
                
                Log.d("BankNoti", "Đã cập nhật TK ${targetAccount.name} | $type $amount")
            } catch (e: Exception) {
                Log.e("BankNoti", "Lỗi xử lý: ${e.message}")
            }
        }
    }
}
