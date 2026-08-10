package com.example.finfit.insights.domain.usecase

import com.example.finfit.insights.domain.model.*
import com.example.finfit.insights.domain.repository.ICrossModuleRepository
import kotlinx.coroutines.flow.Flow

class GetCrossModuleWeeklySummaryUseCase(
    private val repository: ICrossModuleRepository
) {
    operator fun invoke(userId: String): Flow<CrossModuleWeeklySummary> {
        return repository.observeWeeklySummary(userId)
    }
}

class CalculateHealthySavingsUseCase(
    private val repository: ICrossModuleRepository
) {
    operator fun invoke(userId: String): Flow<HealthySavingsPiggybank> {
        return repository.observeHealthySavingsPiggybank(userId)
    }
}

class GetCrossModuleGamificationUseCase(
    private val repository: ICrossModuleRepository
) {
    fun getChallenges(userId: String): Flow<List<CrossModuleChallenge>> {
        return repository.observeChallenges(userId)
    }

    fun getBadges(userId: String): Flow<List<CrossModuleBadge>> {
        return repository.observeBadges(userId)
    }
}

class CheckCrossModuleAlertUseCase(
    private val repository: ICrossModuleRepository
) {
    suspend operator fun invoke(userId: String): CrossModuleAlert? {
        return repository.checkCrossModuleAlert(userId)
    }
}

class LinkMealToTransactionUseCase(
    private val repository: ICrossModuleRepository
) {
    suspend operator fun invoke(mealId: String, transactionId: String) {
        repository.linkMealWithTransaction(mealId, transactionId)
    }
}
