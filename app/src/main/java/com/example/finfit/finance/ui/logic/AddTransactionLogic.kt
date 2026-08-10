package com.example.finfit.finance.ui.logic

import android.net.Uri
import com.example.finfit.finance.model.AppBankAccount
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.model.FinanceBudget
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.GroupPrepaidItem
import com.example.finfit.finance.model.PaymentMethod
import com.example.finfit.finance.model.TransactionType
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─── Kết quả lưu giao dịch ───────────────────────────────────────────────────

/**
 * Kết quả sau khi xây dựng giao dịch mới:
 * - [transaction]     : Giao dịch mới cần lưu lên Firestore
 * - [updatedWallet]   : Ví đã cập nhật số dư (accounts, generalSavings, groupPrepaidItems)
 * - [imageUri]        : URI ảnh đính kèm (nullable)
 */
data class TransactionSaveResult(
    val transaction: FinanceTransaction,
    val updatedWallet: AppUserWallet,
    val imageUri: Uri?
)

// ─── Builder logic giao dịch ─────────────────────────────────────────────────

/**
 * Xây dựng giao dịch mới và cập nhật ví — tách hoàn toàn khỏi UI.
 *
 * Thực hiện 3 bước:
 * 1. Cập nhật số dư các tài khoản (thu/chi/chuyển)
 * 2. Tạo GroupPrepaidItem nếu là chi trả trước cho nhóm
 * 3. Khấu trừ Tiết kiệm chung nếu vượt ngân sách
 *
 * @param wallet              Ví hiện tại
 * @param txType              Loại giao dịch (EXPENSE / INCOME / TRANSFER)
 * @param amount              Số tiền (dạng Double)
 * @param category            Danh mục
 * @param note                Ghi chú
 * @param fromAccount         Tài khoản nguồn
 * @param toAccount           Tài khoản đích (chỉ dùng khi TRANSFER)
 * @param isGroupPrepayment   true nếu là chi trả trước cho nhóm
 * @param personalAmountText  Số tiền cá nhân tự chịu (raw string, rỗng = chia đều)
 * @param participantCount    Số người chia (bao gồm bản thân)
 * @param budgets             Danh sách ngân sách hiện tại
 * @param transactions        Danh sách giao dịch hiện tại (dùng tính budget đã chi)
 * @param imageUri            URI ảnh đính kèm (nullable)
 */
fun buildTransactionResult(
    wallet: AppUserWallet,
    txType: TransactionType,
    amount: Double,
    category: String,
    note: String,
    fromAccount: AppBankAccount?,
    toAccount: AppBankAccount?,
    isGroupPrepayment: Boolean,
    personalAmountText: String,
    participantCount: Int,
    budgets: List<FinanceBudget>,
    transactions: List<FinanceTransaction>,
    imageUri: Uri?
): TransactionSaveResult {
    val txId = UUID.randomUUID().toString()
    val personalAmount = personalAmountText.toDoubleOrNull() ?: (amount / participantCount)
    val groupAmount = (amount - personalAmount).coerceAtLeast(0.0)

    // ─── Xây dựng FinanceTransaction ─────────────────────────────────────────
    val newTx = FinanceTransaction(
        id = txId,
        amount = amount,
        type = if (isGroupPrepayment) TransactionType.GROUP_PREPAYMENT else txType,
        category = if (txType == TransactionType.TRANSFER) "Chuyển tiền" else category,
        note = note,
        timestamp = Timestamp.now(),
        accountId = fromAccount?.id,
        toAccountId = if (txType == TransactionType.TRANSFER) toAccount?.id else null,
        paymentMethod = if (fromAccount?.bankCode == "CASH") PaymentMethod.CASH else PaymentMethod.BANKING,
        isGroupPrepayment = isGroupPrepayment,
        personalAmount = if (isGroupPrepayment) personalAmount else amount,
        groupAmount = if (isGroupPrepayment) groupAmount else 0.0,
        participantCount = if (isGroupPrepayment) participantCount else 1
    )

    // ─── LOGIC 1: Cập nhật số dư tài khoản ───────────────────────────────────
    val updatedAccounts = wallet.accounts.map { acc ->
        when (txType) {
            TransactionType.INCOME -> {
                if (acc.id == fromAccount?.id) acc.copy(amount = acc.amount + amount) else acc
            }
            TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> {
                if (acc.id == fromAccount?.id) acc.copy(amount = acc.amount - amount) else acc
            }
            TransactionType.TRANSFER -> {
                when (acc.id) {
                    fromAccount?.id -> acc.copy(amount = acc.amount - amount)
                    toAccount?.id   -> acc.copy(amount = acc.amount + amount)
                    else            -> acc
                }
            }
        }
    }

    // ─── LOGIC 2: Tạo GroupPrepaidItem nếu trả trước cho nhóm ────────────────
    var finalGroupPrepaidItems = wallet.groupPrepaidItems
    if (isGroupPrepayment && groupAmount > 0) {
        val dateStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())
        val newPrepaidItem = GroupPrepaidItem(
            id = UUID.randomUUID().toString(),
            transactionId = txId,
            description = "${category.ifBlank { "Chia tiền" }} - $dateStr${
                if (note.isNotBlank()) " ($note)" else ""
            }".take(60),
            totalAmount = amount,
            groupOwedAmount = groupAmount,
            participantCount = participantCount,
            createdAt = Timestamp.now()
        )
        finalGroupPrepaidItems = finalGroupPrepaidItems + newPrepaidItem
    }

    // ─── LOGIC 3: Khấu trừ Tiết kiệm chung nếu vượt ngân sách ───────────────
    var finalGeneralSavings = wallet.generalSavings
    if (txType == TransactionType.EXPENSE || isGroupPrepayment) {
        val personalExpense = if (isGroupPrepayment) personalAmount else amount

        val relevantBudgets = budgets.filter {
            it.category == "Tất cả" || it.category == category
        }

        for (budget in relevantBudgets) {
            val totalSpentBefore = transactions.filter { tx ->
                (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT) &&
                    (budget.category == "Tất cả" || tx.category == budget.category) &&
                    tx.timestamp.toDate().after(budget.startDate.toDate())
            }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }

            val budgetLimit = budget.amount
            if (totalSpentBefore + personalExpense > budgetLimit) {
                val excess = if (totalSpentBefore >= budgetLimit) {
                    personalExpense
                } else {
                    (totalSpentBefore + personalExpense) - budgetLimit
                }
                if (excess > 0) {
                    finalGeneralSavings -= excess
                }
            }
        }
    }

    val updatedWallet = wallet.copy(
        accounts = updatedAccounts,
        generalSavings = finalGeneralSavings,
        groupPrepaidItems = finalGroupPrepaidItems
    )

    return TransactionSaveResult(newTx, updatedWallet, imageUri)
}
