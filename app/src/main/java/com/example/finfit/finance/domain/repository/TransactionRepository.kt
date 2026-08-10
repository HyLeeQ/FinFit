package com.example.finfit.finance.domain.repository

import com.example.finfit.finance.model.FinanceTransaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(userId: String, limit: Long = 50): Flow<List<FinanceTransaction>>
    suspend fun getTransactions(userId: String, limit: Long = 50): List<FinanceTransaction>
    suspend fun addTransaction(userId: String, transaction: FinanceTransaction)
    suspend fun updateTransaction(userId: String, transaction: FinanceTransaction)
    suspend fun deleteTransaction(userId: String, transactionId: String)
}
