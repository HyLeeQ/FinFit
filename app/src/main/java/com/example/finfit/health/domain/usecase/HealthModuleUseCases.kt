package com.example.finfit.health.domain.usecase

import com.example.finfit.health.domain.model.FoodMeal
import com.example.finfit.health.domain.model.SleepSession
import com.example.finfit.health.domain.model.WaterLog
import com.example.finfit.health.domain.repository.MealRepository
import com.example.finfit.health.domain.repository.SleepRepository
import com.example.finfit.health.domain.repository.StepRepository
import com.example.finfit.health.domain.repository.WaterRepository

class DetectFoodFromImageUseCase(
    private val mealRepository: MealRepository
) {
    suspend operator fun invoke(meal: FoodMeal) {
        mealRepository.logMeal(meal)
    }
}

class TrackWaterUseCase(
    private val waterRepository: WaterRepository
) {
    suspend operator fun invoke(amountMl: Int) {
        waterRepository.logWater(amountMl)
    }
}

class TrackSleepUseCase(
    private val sleepRepository: SleepRepository
) {
    suspend operator fun invoke(session: SleepSession) {
        sleepRepository.saveSleepSession(session)
    }
}

class SyncStepsUseCase(
    private val stepRepository: StepRepository
) {
    suspend operator fun invoke(steps: Int, calories: Int) {
        stepRepository.saveSteps(steps, calories)
    }
}
