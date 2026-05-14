package com.example.finfit.health.repository

import android.content.Context
import android.util.Log
import com.example.finfit.health.model.SleepSessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class SleepRepository(private val context: Context) {
    private val database = HealthDatabase.getDatabase(context)
    private val sleepLogDao = database.sleepLogDao()
    private val healthDao = database.healthDao()
    private val healthRepository = HealthRepository(context)

    fun observeSleepSessionsForDate(date: String): Flow<List<SleepSessionEntity>> {
        return sleepLogDao.observeSleepSessionsForDate(date)
    }

    suspend fun logSleepSession(
        date: String,
        bedTimeTimestamp: Long,
        wakeTimeTimestamp: Long,
        sleepQuality: Int = 3
    ) {
        // 1. Insert session
        val session = SleepSessionEntity(
            date = date,
            bedTimeTimestamp = bedTimeTimestamp,
            wakeTimeTimestamp = wakeTimeTimestamp,
            sleepQuality = sleepQuality
        )
        sleepLogDao.insertSleepSession(session)

        // 2. Re-calculate total sleep hours for the day
        recalculateAndSaveSummary(date)

        // 3. Update SharedPreferences for bedTime and wakeTime
        val bedCal = Calendar.getInstance().apply { timeInMillis = bedTimeTimestamp }
        val wakeCal = Calendar.getInstance().apply { timeInMillis = wakeTimeTimestamp }
        
        val bedTimeMinute = bedCal.get(Calendar.HOUR_OF_DAY) * 60 + bedCal.get(Calendar.MINUTE)
        val wakeTimeMinute = wakeCal.get(Calendar.HOUR_OF_DAY) * 60 + wakeCal.get(Calendar.MINUTE)

        val prefs = context.getSharedPreferences("HealthPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("bed_time_minute", bedTimeMinute)
            .putInt("wake_time_minute", wakeTimeMinute)
            .apply()

        // 4. Update the reminder alarm if water reminder is enabled
        if (healthRepository.isWaterReminderEnabled()) {
            com.example.finfit.health.manager.WaterReminderManager.scheduleReminder(context)
        }
    }

    suspend fun deleteSleepSession(sessionId: String, date: String) {
        sleepLogDao.deleteSleepSession(sessionId)
        recalculateAndSaveSummary(date)
    }

    private suspend fun recalculateAndSaveSummary(date: String) {
        val totalMillis = sleepLogDao.getTotalSleepDurationMillisForDate(date) ?: 0L
        val sleepHours = totalMillis / (1000f * 60 * 60)

        // Update HealthEntity (Summary)
        val healthEntity = healthDao.getHealthByDate(date)
        if (healthEntity != null) {
            val updated = healthEntity.copy(
                sleepHours = sleepHours,
                syncStatus = 0 // Cần đồng bộ lên Cloud
            )
            healthDao.insertHealth(updated)
        } else {
            // Tạo mới nếu chưa có
            val newHealth = com.example.finfit.health.model.HealthEntity(
                date = date,
                sleepHours = sleepHours,
                syncStatus = 0,
                lastUpdated = System.currentTimeMillis()
            )
            healthDao.insertHealth(newHealth)
        }
    }
}
