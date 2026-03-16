package com.example.finfit.health.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finfit.health.model.StepEntity

@Dao
interface StepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(step: StepEntity)

    @Query("SELECT * FROM step_history WHERE date = :date")
    suspend fun getStepsByDate(date: String): StepEntity?
}
