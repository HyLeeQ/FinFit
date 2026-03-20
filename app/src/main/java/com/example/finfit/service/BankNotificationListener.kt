package com.example.finfit.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.model.AppUserWallet
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

    // Map các package thông dụng ở VN sang BankCode
    private val packageToBankCode = mapOf(
        "com.mbmobile" to "MB",
        "com.vcb.digibank" to "VIETCOMBANK",
        "com.mservice.momotransfer" to "MOMO",
        "vn.com.techcombank.bb.app" to "TECHCOMBANK",
        "com.techcombank.retail.mb" to "TECHCOMBANK",
        "com.vnpay.bidv" to "BIDV",
        "com.vnpay.vpbank" to "VPBANK",
        "com.vnpay.agribank" to "AGRIBANK",
        "com.tpb.mb.retail" to "TPBANK",
        "com.zing.zalo" to "ZALOPAY",
        "com.fpt.viettelpay" to "VIETTELPAY"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val bankCode = packageToBankCode[packageName] ?: return
        
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""
        val content = "$title $text".replace("\n", " ")

        Log.d("BankNoti", "Nhận thông báo từ $bankCode: $content")

        // Parse số tiền và loại giao dịch
        val result = parseBankingContent(content) ?: return
        val amount = result.first
        val type = result.second

        processTransaction(bankCode, amount, type, content)
    }

    private fun parseBankingContent(content: String): Pair<Double, TransactionType>? {
        // Regex tìm số tiền có dấu + hoặc - hoặc các từ khóa biến động
        // Ví dụ: +100,000VND, -50.000đ, "Số dư thay đổi +200,000"
        val cleanContent = content.replace(",", "")
        
        // Tìm số tiền (chuỗi số liên tục)
        val amountRegex = """(?:\+|\-| biến động |GD: )(\d+)(?:\s?VND|\s?đ| đ)?""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(cleanContent) ?: return null
        
        val value = match.groups[1]?.value?.toDoubleOrNull() ?: return null
        if (value < 1000) return null // Bỏ qua các giao dịch quá nhỏ

        val isIncome = content.contains("+") || 
                       content.contains("nhận", ignoreCase = true) || 
                       content.contains("vào", ignoreCase = true) ||
                       content.contains("tang", ignoreCase = true)
                       
        val isExpense = content.contains("-") || 
                        content.contains("trừ", ignoreCase = true) || 
                        content.contains("thanh toán", ignoreCase = true) ||
                        content.contains("chuyển tiền", ignoreCase = true)

        return when {
            isIncome -> Pair(value, TransactionType.INCOME)
            isExpense -> Pair(value, TransactionType.EXPENSE)
            else -> Pair(value, TransactionType.EXPENSE) // Mặc định là chi tiêu nếu không rõ
        }
    }

    private fun processTransaction(bankCode: String, amount: Double, type: TransactionType, note: String) {
        val user = authRepository.getCurrentUser() ?: return
        
        serviceScope.launch {
            try {
                val wallet = firestoreRepository.getUserWallet(user.uid) ?: return@launch
                
                // Tìm tài khoản phù hợp với bankCode trong ví
                val targetAccount = wallet.accounts.find { it.bankCode == bankCode } 
                                     ?: wallet.accounts.firstOrNull() // Nếu không khớp bankCode, lấy TK đầu tiên
                                     ?: return@launch
                
                val updatedAccounts = wallet.accounts.map {
                    if (it.id == targetAccount.id) {
                        val newAmount = if (type == TransactionType.INCOME) it.amount + amount else it.amount - amount
                        it.copy(amount = newAmount)
                    } else it
                }
                
                val updatedWallet = wallet.copy(accounts = updatedAccounts)
                
                // 1. Lưu ví mới
                firestoreRepository.saveUserWallet(updatedWallet)
                
                // 2. Tạo record giao dịch vào lịch sử
                val transaction = FinanceTransaction(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = type,
                    category = if (type == TransactionType.INCOME) "Thu nhập khác" else "Chi tiêu khác",
                    note = "[Tự động] $note",
                    paymentMethod = com.example.finfit.finance.model.PaymentMethod.BANKING
                )
                
                // Sử dụng đoạn mã bạn cung cấp để lưu giao dịch
                if (user != null) {
                    firestoreRepository.addTransaction(user.uid, transaction)
                }
                
                Log.d("BankNoti", "Tự động cập nhật thành công: ${targetAccount.name} | $type $amount")
            } catch (e: Exception) {
                Log.e("BankNoti", "Lỗi xử lý tự động: ${e.message}")
            }
        }
    }
}
