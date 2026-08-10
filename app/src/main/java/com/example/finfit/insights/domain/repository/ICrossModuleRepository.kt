package com.example.finfit.insights.domain.repository

import com.example.finfit.insights.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ICrossModuleRepository {
    fun observeWeeklySummary(userId: String): Flow<CrossModuleWeeklySummary>
    fun observeHealthySavingsPiggybank(userId: String): Flow<HealthySavingsPiggybank>
    fun observeChallenges(userId: String): Flow<List<CrossModuleChallenge>>
    fun observeBadges(userId: String): Flow<List<CrossModuleBadge>>
    suspend fun checkCrossModuleAlert(userId: String): CrossModuleAlert?
    suspend fun linkMealWithTransaction(mealId: String, transactionId: String)
}
