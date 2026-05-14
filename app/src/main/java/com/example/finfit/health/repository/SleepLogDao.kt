package com.example.finfit.health.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finfit.health.model.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepSession(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_session_logs WHERE date = :date AND isDeleted = 0 ORDER BY bedTimeTimestamp DESC")
    fun observeSleepSessionsForDate(date: String): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_session_logs WHERE date = :date AND isDeleted = 0 ORDER BY bedTimeTimestamp DESC")
    suspend fun getSleepSessionsForDate(date: String): List<SleepSessionEntity>

    @Query("UPDATE sleep_session_logs SET isDeleted = 1 WHERE id = :sessionId")
    suspend fun deleteSleepSession(sessionId: String)

    @Query("SELECT SUM(wakeTimeTimestamp - bedTimeTimestamp) FROM sleep_session_logs WHERE date = :date AND isDeleted = 0")
    suspend fun getTotalSleepDurationMillisForDate(date: String): Long?
}
