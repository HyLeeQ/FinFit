package com.example.finfit.health.domain.repository

import com.example.finfit.health.domain.model.FoodMeal
import com.example.finfit.health.domain.model.SleepSession
import com.example.finfit.health.domain.model.StepData
import com.example.finfit.health.domain.model.WaterLog
import kotlinx.coroutines.flow.Flow

interface StepRepository {
    fun observeDailySteps(): Flow<StepData?>
    suspend fun saveSteps(steps: Int, calories: Int)
}

interface WaterRepository {
    fun observeWaterLogs(): Flow<List<WaterLog>>
    suspend fun logWater(amountMl: Int)
}

interface SleepRepository {
    fun observeSleepSessions(): Flow<List<SleepSession>>
    suspend fun saveSleepSession(session: SleepSession)
}

interface MealRepository {
    fun observeMeals(): Flow<List<FoodMeal>>
    suspend fun logMeal(meal: FoodMeal)
}
