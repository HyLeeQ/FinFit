package com.example.finfit.health.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finfit.health.model.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(step: StepEntity)

    @Query("SELECT * FROM step_history WHERE date = :date")
    suspend fun getStepsByDate(date: String): StepEntity?

    /** Reactive query — Room tự emit mỗi khi bảng step_history thay đổi */
    @Query("SELECT * FROM step_history WHERE date = :date")
    fun observeStepsByDate(date: String): Flow<StepEntity?>

    @Query("SELECT * FROM step_history WHERE syncStatus != 2")
    suspend fun getUnsyncedSteps(): List<StepEntity>

    @Query("UPDATE step_history SET syncStatus = :status WHERE date = :date")
    suspend fun updateSyncStatus(date: String, status: Int)

    @Query("DELETE FROM step_history")
    suspend fun deleteAllSteps()
}
