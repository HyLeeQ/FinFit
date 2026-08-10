package com.example.finfit.finance.data.repository

import com.example.finfit.finance.domain.repository.TransactionRepository
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.repository.FirestoreRepository
import kotlinx.coroutines.flow.Flow

class TransactionRepositoryImpl(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : TransactionRepository {

    override fun observeTransactions(userId: String, limit: Long): Flow<List<FinanceTransaction>> {
        return firestoreRepository.observeTransactions(userId, limit)
    }

    override suspend fun getTransactions(userId: String, limit: Long): List<FinanceTransaction> {
        return firestoreRepository.getTransactions(userId, limit)
    }

    override suspend fun addTransaction(userId: String, transaction: FinanceTransaction) {
        firestoreRepository.addTransaction(userId, transaction)
    }

    override suspend fun updateTransaction(userId: String, transaction: FinanceTransaction) {
        firestoreRepository.updateTransaction(userId, transaction)
    }

    override suspend fun deleteTransaction(userId: String, transactionId: String) {
        firestoreRepository.deleteTransaction(userId, transactionId)
    }
}
