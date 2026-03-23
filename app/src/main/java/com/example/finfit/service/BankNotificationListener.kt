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

        // Parse số tiền, loại giao dịch và phát hiện chuyển khoản tự động
        val result = parseBankingContent(content) ?: return
        val amount = result.first
        val type = result.second
        val detectedTransferTo = result.third

        processTransaction(bankCode, amount, type, content, detectedTransferTo)
    }

    private fun parseBankingContent(content: String): Triple<Double, TransactionType, String?>? {
        val cleanContent = content.replace(",", "")
        
        val amountRegex = """(?:\+|\-| biến động |GD: )(\d+)(?:\s?VND|\s?đ| đ)?""".toRegex(RegexOption.IGNORE_CASE)
        val match = amountRegex.find(cleanContent) ?: return null
        
        val value = match.groups[1]?.value?.toDoubleOrNull() ?: return null
        if (value < 1000) return null

        // Phân hệ nhận diện chuyển khoản nội bộ (tự nạp ví của mình)
        // VD: "MOMO-CASHIN-..." là nạp vào ví của mình. "MOMO-TRANSFER" là chuyển cho người khác.
        val isMomoTransfer = content.contains("MOMO-CASHIN", ignoreCase = true) || content.contains("NAP TIEN MOMO", ignoreCase = true)
        val isZaloTransfer = content.contains("ZALOPAY-CASHIN", ignoreCase = true) || content.contains("NAP TIEN ZALOPAY", ignoreCase = true)
        val isAtmTransfer = content.contains("ATM", ignoreCase = true) || content.contains("RUT TIEN", ignoreCase = true)
        
        val isIncome = content.contains("+") || 
                       content.contains("nhận", ignoreCase = true) || 
                       content.contains("vào", ignoreCase = true) ||
                       content.contains("tang", ignoreCase = true)
                       
        val isExpense = content.contains("-") || 
                        content.contains("trừ", ignoreCase = true) || 
                        content.contains("thanh toán", ignoreCase = true) ||
                        content.contains("chuyển tiền", ignoreCase = true) ||
                        isMomoTransfer || isZaloTransfer || isAtmTransfer

        val type = when {
            (isMomoTransfer || isZaloTransfer || isAtmTransfer) && isExpense -> TransactionType.TRANSFER
            isIncome -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
        
        val targetBankCode = when {
            isMomoTransfer -> "MOMO"
            isZaloTransfer -> "ZALOPAY"
            isAtmTransfer -> "CASH"
            else -> null
        }

        return Triple(value, type, targetBankCode)
    }

    private fun processTransaction(bankCode: String, amount: Double, type: TransactionType, note: String, transferTo: String?) {
        val user = authRepository.getCurrentUser() ?: return
        
        serviceScope.launch {
            try {
                val wallet = firestoreRepository.getUserWallet(user.uid) ?: return@launch
                
                // Tìm tài khoản nguồn (của bank nào nhận thông báo)
                val sourceAccount = wallet.accounts.find { it.bankCode == bankCode } 
                                     ?: wallet.accounts.firstOrNull()
                                     ?: return@launch
                
                // Tìm tài khoản đích (nếu là chuyển khoản tự động)
                val destinationAccount = if (type == TransactionType.TRANSFER && transferTo != null) {
                    wallet.accounts.find { it.bankCode == transferTo }
                } else null

                val updatedAccounts = wallet.accounts.map {
                    when (it.id) {
                        sourceAccount.id -> {
                            val newAmount = if (type == TransactionType.INCOME) it.amount + amount else it.amount - amount
                            it.copy(amount = newAmount)
                        }
                        destinationAccount?.id -> {
                            // Nếu nguồn là EXPENSE (TRANSFER OUT), đích sẽ nhận được tiền (+ amount)
                            it.copy(amount = it.amount + amount)
                        }
                        else -> it
                    }
                }
                
                val updatedWallet = wallet.copy(accounts = updatedAccounts)
                firestoreRepository.saveUserWallet(updatedWallet)
                
                // Tạo record giao dịch
                val transaction = FinanceTransaction(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = type,
                    category = when(type) {
                        TransactionType.INCOME -> "Thu nhập tự động"
                        TransactionType.TRANSFER -> "Chuyển khoản tự động"
                        else -> "Chi tiêu tự động"
                    },
                    note = "[Tự động] $note",
                    paymentMethod = PaymentMethod.BANKING,
                    accountId = sourceAccount.id,
                    toAccountId = destinationAccount?.id
                )
                
                firestoreRepository.addTransaction(user.uid, transaction)
                
                Log.d("BankNoti", "Tự động xử lý: Source=${sourceAccount.name} | Dest=${destinationAccount?.name} | $type $amount")
            } catch (e: Exception) {
                Log.e("BankNoti", "Lỗi xử lý tự động: ${e.message}")
            }
        }
    }
}
