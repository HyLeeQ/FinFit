package com.example.finfit.ui.assistant

import android.net.Uri
import com.example.finfit.finance.model.DebtLoan
import com.example.finfit.finance.model.FinanceBudget
import com.example.finfit.finance.model.SavingsGoal
import com.example.finfit.finance.model.SpendingScheduleItem
import com.example.finfit.finance.util.ParsedTransaction
import java.util.UUID

sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class TransactionCard(val parsed: ParsedTransaction, val confirmed: Boolean = false) : MessageContent()
    data class BillCard(val imageUri: Uri, val extractedAmount: Double?, val confirmed: Boolean = false) : MessageContent()
    data class DebtCard(val debt: DebtLoan, val confirmed: Boolean = false) : MessageContent()
    data class SavingsCard(val goal: SavingsGoal, val confirmed: Boolean = false) : MessageContent()
    data class BudgetCard(val budget: FinanceBudget, val confirmed: Boolean = false) : MessageContent()
    data class ScheduleCard(val item: SpendingScheduleItem, val confirmed: Boolean = false) : MessageContent()
    data class SplitBillCard(val totalAmount: Double, val participantCount: Int, val category: String, val note: String, val confirmed: Boolean = false, val initialParticipants: List<com.example.finfit.finance.model.TransactionParticipant> = emptyList()) : MessageContent()
    data class HeldFundCard(val fundName: String, val amount: Double, val confirmed: Boolean = false) : MessageContent()
    data class HabitUpdateCard(val habit: com.example.finfit.finance.model.UserHabit, val confirmed: Boolean = false) : MessageContent()
    data class WeeklyPlanCard(val description: String, val items: List<SpendingScheduleItem>, val confirmed: Boolean = false) : MessageContent()
    data class DepositSavingsCard(val goalName: String, val amount: Double, val walletSource: String?, val confirmed: Boolean = false) : MessageContent()
    data class WithdrawSavingsCard(val goalName: String, val amount: Double, val destinationWallet: String?, val transferToSavingsGoal: String?, val confirmed: Boolean = false) : MessageContent()
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: MessageContent,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
