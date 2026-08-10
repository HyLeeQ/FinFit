package com.example.finfit.finance.data.repository

import com.example.finfit.finance.domain.repository.SavingsRepository
import com.example.finfit.finance.model.SavingsGoal
import com.example.finfit.finance.repository.FirestoreRepository
import kotlinx.coroutines.flow.Flow

class SavingsRepositoryImpl(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : SavingsRepository {

    override fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>> {
        return firestoreRepository.observeSavingsGoals(userId)
    }

    override suspend fun getSavingsGoals(userId: String): List<SavingsGoal> {
        return firestoreRepository.getSavingsGoals(userId)
    }

    override suspend fun saveSavingsGoal(userId: String, goal: SavingsGoal) {
        firestoreRepository.saveSavingsGoal(userId, goal)
    }

    override suspend fun deleteSavingsGoal(userId: String, goalId: String) {
        firestoreRepository.deleteSavingsGoal(userId, goalId)
    }
}
