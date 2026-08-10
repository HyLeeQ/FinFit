package com.example.finfit.health.domain.usecase

import com.example.finfit.health.domain.repository.IHealthRepository
import com.example.finfit.health.model.HealthEntity
import kotlinx.coroutines.flow.Flow

class SyncHealthDataUseCase(private val healthRepository: IHealthRepository) {
    suspend operator fun invoke() {
        healthRepository.syncCloudToLocal()
        healthRepository.syncLocalToCloud()
    }
}

class GetTodayHealthUseCase(private val healthRepository: IHealthRepository) {
    operator fun invoke(): Flow<HealthEntity?> = healthRepository.getTodayHealthData()
}
