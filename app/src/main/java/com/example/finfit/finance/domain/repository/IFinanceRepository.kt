package com.example.finfit.finance.domain.repository

import com.example.finfit.finance.model.*
import kotlinx.coroutines.flow.Flow

interface IFinanceRepository {
    // Wallet
    fun observeUserWallet(uid: String): Flow<AppUserWallet?>
    suspend fun getUserWallet(uid: String): AppUserWallet?
    suspend fun saveUserWallet(wallet: AppUserWallet)

    // Transactions
    fun observeTransactions(uid: String, limit: Long = 50): Flow<List<FinanceTransaction>>
    suspend fun addTransaction(uid: String, transaction: FinanceTransaction)
    suspend fun updateTransaction(uid: String, transaction: FinanceTransaction)
    suspend fun deleteTransaction(uid: String, transactionId: String)

    // Budgets
    fun observeBudgets(uid: String): Flow<List<FinanceBudget>>
    suspend fun saveBudget(uid: String, budget: FinanceBudget)
    suspend fun deleteBudget(uid: String, budgetId: String)

    // Savings Goals
    fun observeSavingsGoals(uid: String): Flow<List<SavingsGoal>>
    suspend fun saveSavingsGoal(uid: String, goal: SavingsGoal)
    suspend fun deleteSavingsGoal(uid: String, goalId: String)

    // Debt & Loan
    fun observeDebtLoans(uid: String): Flow<List<DebtLoan>>
    suspend fun saveDebtLoan(uid: String, debtLoan: DebtLoan)
    suspend fun deleteDebtLoan(uid: String, debtLoanId: String)
    suspend fun toggleDebtLoanPaidStatus(uid: String, debtLoanId: String, isPaid: Boolean)

    // Recurring Transactions
    fun observeRecurringTransactions(uid: String): Flow<List<RecurringTransaction>>
    suspend fun saveRecurringTransaction(uid: String, recurring: RecurringTransaction)
    suspend fun deleteRecurringTransaction(uid: String, id: String)

    // Saved Split Groups
    fun observeSavedSplitGroups(uid: String): Flow<List<SavedSplitGroup>>
    suspend fun saveSplitGroup(uid: String, group: SavedSplitGroup)
    suspend fun deleteSplitGroup(uid: String, groupId: String)

    // Category Rules
    fun observeCategoryRules(uid: String): Flow<List<CategoryRule>>
    suspend fun saveCategoryRule(uid: String, rule: CategoryRule)
}
