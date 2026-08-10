package com.example.finfit.finance.data.repository

import com.example.finfit.finance.domain.repository.BudgetRepository
import com.example.finfit.finance.model.FinanceBudget
import com.example.finfit.finance.repository.FirestoreRepository
import kotlinx.coroutines.flow.Flow

class BudgetRepositoryImpl(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : BudgetRepository {

    override fun observeBudgets(userId: String): Flow<List<FinanceBudget>> {
        return firestoreRepository.observeBudgets(userId)
    }

    override suspend fun saveBudget(userId: String, budget: FinanceBudget) {
        firestoreRepository.saveBudget(userId, budget)
    }

    override suspend fun deleteBudget(userId: String, budgetId: String) {
        firestoreRepository.deleteBudget(userId, budgetId)
    }
}
