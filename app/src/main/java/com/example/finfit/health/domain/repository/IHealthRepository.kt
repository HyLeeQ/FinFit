package com.example.finfit.health.domain.repository

import com.example.finfit.health.model.HealthEntity
import kotlinx.coroutines.flow.Flow

interface IHealthRepository {
    suspend fun syncCloudToLocal()
    suspend fun syncLocalToCloud()
    fun getTodayHealthData(): Flow<HealthEntity?>
    suspend fun updateSteps(steps: Int, calories: Int)
}
