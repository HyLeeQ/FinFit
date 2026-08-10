package com.example.finfit.finance.domain.repository

import com.example.finfit.finance.model.FinanceBudget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeBudgets(userId: String): Flow<List<FinanceBudget>>
    suspend fun saveBudget(userId: String, budget: FinanceBudget)
    suspend fun deleteBudget(userId: String, budgetId: String)
}
