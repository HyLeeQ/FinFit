package com.example.finfit.health.repository

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.finfit.health.data.HealthSyncWorker
import com.example.finfit.health.model.HealthUiState
import com.example.finfit.health.model.toUiState
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * HealthViewModel — Cung cấp dữ liệu sức khỏe reactive cho UI.
 *
 * Merge 2 nguồn dữ liệu:
 *   1. StepCounterManager (sensor realtime)
 *   2. HealthDao.observeHealthByDate (Room persistent)
 * → Kết hợp qua extension function toUiState() → UI luôn hiển thị con số cao nhất.
 */
class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val healthDao = HealthDatabase.getDatabase(application).healthDao()
    private val stepCounterManager = StepCounterManager.getInstance(application)
    private val healthRepository = HealthRepository(application)
    private val workManager = WorkManager.getInstance(application)

    private val _healthUiState = MutableStateFlow(HealthUiState())
    val healthUiState: StateFlow<HealthUiState> = _healthUiState.asStateFlow()

    // Giữ todaySteps riêng để HealthDashboard dùng (backward compatible)
    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _achievementEvent = MutableSharedFlow<String>()
    val achievementEvent: SharedFlow<String> = _achievementEvent.asSharedFlow()

    private val isFirst1000AchievedState = MutableStateFlow(false)
    private val hasCelebrated1000State = MutableStateFlow(false)

    fun mark1000StepsCelebrated() {
        viewModelScope.launch(Dispatchers.IO) {
            healthRepository.markFirst1000StepsCelebrated()
            hasCelebrated1000State.value = true
        }
    }

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
            isFirst1000AchievedState.value = healthRepository.checkFirst1000StepsAchieved()
            hasCelebrated1000State.value = healthRepository.checkFirst1000StepsCelebrated()
        }

        viewModelScope.launch {
            val today = getCurrentDate()

            val achievementFlow = combine(isFirst1000AchievedState, hasCelebrated1000State) { a, c -> Pair(a, c) }

            // Combine tất cả nguồn sensor realtime + Room persistent
            combine(
                stepCounterManager.todaySteps,
                stepCounterManager.calories,
                stepCounterManager.activeMinutes,
                healthDao.observeHealthByDate(today),
                achievementFlow
            ) { sensorSteps, sensorCal, sensorMinutes, dbEntity, achPair ->
                dbEntity.toUiState(
                    sensorSteps = sensorSteps,
                    sensorCaloriesOut = sensorCal,
                    sensorActiveMinutes = sensorMinutes
                ).copy(
                    isFirst1000StepsAchieved = achPair.first,
                    hasCelebrated1000Steps = achPair.second
                )
            }.collect { state ->
                // Check achievement logic
                if (state.steps >= 1000 && !state.isFirst1000StepsAchieved) {
                    isFirst1000AchievedState.value = true
                    viewModelScope.launch(Dispatchers.IO) {
                        healthRepository.markFirst1000StepsAchieved()
                    }
                    showAchievementNotification()
                }

                _healthUiState.value = state.copy(
                    isFirst1000StepsAchieved = isFirst1000AchievedState.value,
                    hasCelebrated1000Steps = hasCelebrated1000State.value
                )
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

        val syncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(30, TimeUnit.MINUTES)
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
     * Reset chỉ bước chân ngày hôm nay (giữ nguyên nước/calo/sleep)
     */
    fun resetTodaySteps(onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            healthRepository.resetTodaySteps()
            // Reset lại sensor manager
            stepCounterManager.resetInMemoryState()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Đồng bộ thủ công với callback để hiển thị thông báo
     */
    fun forceSyncWithCallback(onComplete: () -> Unit = {}) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<HealthSyncWorker>()
            .setConstraints(constraints)
            .build()

        viewModelScope.launch(Dispatchers.IO) {
            stepCounterManager.flushToDatabase()
            healthRepository.syncCloudToLocal()
            workManager.enqueue(syncRequest)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Thêm nước (Ghi trực tiếp vào DB thông qua Repository)
     */
    fun addWater(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            healthRepository.updateWaterConsumption(amount)
        }
    }

    /**
     * Thêm calo nạp vào (cho module thực phẩm sau này)
     */
    fun addCaloriesIn(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            healthRepository.updateCaloriesIn(amount)
        }
    }

    private fun showAchievementNotification() {
        val context = getApplication<Application>()
        val channelId = "health_achievements"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Thành tựu sức khỏe",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setContentTitle("🎉 Chúc mừng!")
            .setContentText("Bạn đã hoàn thành 1.000 bước đi đầu tiên!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1000, notification)
    }
}
