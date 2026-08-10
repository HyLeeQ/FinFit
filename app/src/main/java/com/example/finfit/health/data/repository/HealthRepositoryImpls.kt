package com.example.finfit.health.data.repository

import com.example.finfit.health.domain.model.FoodMeal
import com.example.finfit.health.domain.model.SleepSession
import com.example.finfit.health.domain.model.StepData
import com.example.finfit.health.domain.model.WaterLog
import com.example.finfit.health.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepRepositoryImpl : StepRepository {
    private val _stepData = MutableStateFlow<StepData?>(StepData(steps = 7500, calories = 320, distanceMeters = 5400.0, date = "Today"))

    override fun observeDailySteps(): Flow<StepData?> = _stepData.asStateFlow()

    override suspend fun saveSteps(steps: Int, calories: Int) {
        _stepData.value = StepData(steps = steps, calories = calories, distanceMeters = (steps * 0.72), date = "Today")
    }
}

class WaterRepositoryImpl : WaterRepository {
    private val _waterLogs = MutableStateFlow<List<WaterLog>>(emptyList())

    override fun observeWaterLogs(): Flow<List<WaterLog>> = _waterLogs.asStateFlow()

    override suspend fun logWater(amountMl: Int) {
        _waterLogs.value = _waterLogs.value + WaterLog(amountMl = amountMl)
    }
}

class SleepRepositoryImpl : SleepRepository {
    private val _sleepSessions = MutableStateFlow<List<SleepSession>>(emptyList())

    override fun observeSleepSessions(): Flow<List<SleepSession>> = _sleepSessions.asStateFlow()

    override suspend fun saveSleepSession(session: SleepSession) {
        _sleepSessions.value = _sleepSessions.value + session
    }
}

class MealRepositoryImpl : MealRepository {
    private val _meals = MutableStateFlow<List<FoodMeal>>(
        listOf(
            FoodMeal(id = "1", name = "Phở Bò", calories = 480.0, protein = 25.0, fat = 12.0, carbs = 65.0, mealType = "BREAKFAST", isHomeCooked = false),
            FoodMeal(id = "2", name = "Cơm Gà Luộc & Rau Củ", calories = 520.0, protein = 42.0, fat = 8.0, carbs = 60.0, mealType = "LUNCH", isHomeCooked = true),
            FoodMeal(id = "3", name = "Salad Cá Ngừ", calories = 350.0, protein = 30.0, fat = 10.0, carbs = 20.0, mealType = "DINNER", isHomeCooked = true)
        )
    )

    override fun observeMeals(): Flow<List<FoodMeal>> = _meals.asStateFlow()

    override suspend fun logMeal(meal: FoodMeal) {
        _meals.value = _meals.value + meal
    }
}
