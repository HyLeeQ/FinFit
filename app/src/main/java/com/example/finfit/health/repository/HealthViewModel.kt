package com.example.finfit.health.repository

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finfit.health.data.HealthSyncWorker
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Trạng thái UI tổng hợp cho Health StepCounter screen.
 */
data class HealthUiState(
    val steps: Int = 0,
    val calories: Int = 0,
    val activeMinutes: Int = 0,
    val stepGoal: Int = 1000,
    val activeMinuteGoal: Int = 60,
    val waterConsumedMl: Int = 1600,
    val waterGoalMl: Int = 2000
)

/**
 * HealthViewModel — Cung cấp dữ liệu sức khỏe reactive cho UI.
 *
 * Merge 2 nguồn dữ liệu:
 *   1. StepCounterManager (sensor realtime)
 *   2. StepDao.observeStepsByDate (Room persistent)
 * → Lấy giá trị max() → UI luôn hiển thị con số cao nhất.
 */
class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val stepDao = HealthDatabase.getDatabase(application).stepDao()
    private val stepCounterManager = StepCounterManager.getInstance(application)
    private val healthRepository = HealthRepository(application)
    private val workManager = WorkManager.getInstance(application)

    /** Mục tiêu bước chân */
    val stepGoal: Int = 1000

    private val _healthUiState = MutableStateFlow(HealthUiState())
    val healthUiState: StateFlow<HealthUiState> = _healthUiState.asStateFlow()

    // Giữ todaySteps riêng để HealthDashboard dùng (backward compatible)
    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    // Mock Data: Cột Nước uống
    private val _waterConsumed = MutableStateFlow(1600)
    val waterGoalMl: Int = 2000

    init {
        // Đồng bộ Cloud -> Local ngay khi khởi tạo
        viewModelScope.launch(Dispatchers.IO) {
            healthRepository.syncCloudToLocal()
        }

        // Lên lịch đồng bộ nền tự động
        schedulePeriodicSync()

        // Ticker theo dõi giao thừa (Rollover Tracker), tick mỗi 60s
        viewModelScope.launch {
            while (true) {
                val rolledOver = stepCounterManager.checkAndResetDate()
                if (rolledOver) {
                    // Cú huých chót: Giao thừa cất sổ, bắt buộc đẩy Data hôm qua lên Firebase
                    forceSync()
                }
                kotlinx.coroutines.delay(60_000L)
            }
        }

        viewModelScope.launch {
            val today = getCurrentDate()

            // Combine tất cả nguồn sensor realtime + Room persistent + Nước (Mock)
            combine(
                stepCounterManager.todaySteps,
                stepCounterManager.calories,
                stepCounterManager.activeMinutes,
                stepDao.observeStepsByDate(today).map { entity ->
                    Triple(entity?.steps ?: 0, entity?.calories ?: 0, entity?.activeMinutes ?: 0)
                },
                _waterConsumed
            ) { sensorSteps, sensorCal, sensorMinutes, dbTriple, waterVal ->
                val (dbSteps, dbCal, dbMinutes) = dbTriple
                HealthUiState(
                    steps = maxOf(sensorSteps, dbSteps),
                    calories = maxOf(sensorCal, dbCal),
                    activeMinutes = maxOf(sensorMinutes, dbMinutes),
                    stepGoal = stepGoal,
                    activeMinuteGoal = 60,
                    waterConsumedMl = waterVal,
                    waterGoalMl = waterGoalMl
                )
            }.collect { state ->
                _healthUiState.value = state
                _todaySteps.value = state.steps
            }
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "HealthPeriodicSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Đồng bộ thủ công (OneTime), có thể gọi từ UI như nút "Đồng bộ"
     */
    fun forceSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncRequest = OneTimeWorkRequestBuilder<HealthSyncWorker>()
            .setConstraints(constraints)
            .build()

        // Đảm bảo dữ liệu từ Sensor đang đệm trên RAM phải xả ngay vào DB
        viewModelScope.launch(Dispatchers.IO) {
            stepCounterManager.flushToDatabase()
            
            // Kéo Cloud -> Local trước, sau đó worker đẩy Local -> Cloud
            healthRepository.syncCloudToLocal()
            workManager.enqueue(syncRequest)
        }
    }

    /**
     * Xoá sạch mọi dữ liệu sức khoẻ và reset giao diện
     */
    fun wipeData() {
        viewModelScope.launch(Dispatchers.IO) {
            healthRepository.wipeAllHealthData()
        }
    }

    /**
     * Thêm nước (Mock function, update tạm thời RAM)
     */
    fun addWater(amount: Int) {
        val current = _waterConsumed.value
        _waterConsumed.value = current + amount
    }
}
