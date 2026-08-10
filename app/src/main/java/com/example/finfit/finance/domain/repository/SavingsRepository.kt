package com.example.finfit.finance.domain.repository

import com.example.finfit.finance.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsRepository {
    fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>>
    suspend fun getSavingsGoals(userId: String): List<SavingsGoal>
    suspend fun saveSavingsGoal(userId: String, goal: SavingsGoal)
    suspend fun deleteSavingsGoal(userId: String, goalId: String)
}
