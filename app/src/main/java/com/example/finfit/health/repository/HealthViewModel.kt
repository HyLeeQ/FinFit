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
import com.example.finfit.health.model.DrinkType
import com.example.finfit.health.model.HealthUiState
import com.example.finfit.health.model.SleepUiState
import com.example.finfit.health.model.WaterLogUiItem
import com.example.finfit.health.model.WaterScreenData
import com.example.finfit.health.model.WaterSource
import com.example.finfit.health.model.WaterUiState
import com.example.finfit.health.model.toUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val sleepRepository = SleepRepository(application)
    private val mealRepository = MealRepository()

    /**
     * WaterRepository — Inject todayStepsProvider để Context Enrichment.
     * Mỗi khi logWater() được gọi, nó tự động bắt số bước hiện tại để lưu vào
     * WaterLogEntity.contextSteps (phục vụ phân tích tương quan nước/vận động sau này).
     */
    private val waterRepository = WaterRepository(
        context = application,
        todayStepsProvider = { stepCounterManager.todaySteps.value }
    )

    private val _healthUiState = MutableStateFlow(HealthUiState())
    val healthUiState: StateFlow<HealthUiState> = _healthUiState.asStateFlow()

    // Giữ todaySteps riêng để HealthDashboard dùng (backward compatible)
    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _achievementEvent = MutableSharedFlow<String>()
    val achievementEvent: SharedFlow<String> = _achievementEvent.asSharedFlow()

    private val isFirst1000AchievedState = MutableStateFlow(false)
    private val hasCelebrated1000State = MutableStateFlow(false)

    // ================================================================
    // WATER MODULE STATE
    // ================================================================

    /**
     * Ngày đang xem trên WaterTrackerScreen.
     * Thay đổi qua changeSelectedWaterDate(). Mặc định = hôm nay.
     * Đây là nguồn điều khiển (Source) cho toàn bộ Water reactive stream.
     */
    private val _selectedWaterDate = MutableStateFlow(getCurrentDate())

    /** Trạng thái bật/tắt Reminder trong bộ nhớ Local */
    private val _isReminderEnabled = MutableStateFlow(healthRepository.isWaterReminderEnabled())

    /**
     * waterUiState — StateFlow chính cho WaterTrackerScreen.
     *
     * Luồng hoạt động:
     *   _selectedWaterDate hoặc _isReminderEnabled thay đổi
     *   -> flatMapLatest hủy subscription cũ, mở subscription mới cho ngày mới
     *   -> combine(Summary stream, Logs stream) -> map sang WaterScreenData
     *   -> emit WaterUiState.Ready
     *
     * UI subscribe 1 lần duy nhất, tự động re-render khi:
     *   - Ngày được đổi (changeSelectedWaterDate)
     *   - Có log mới được thêm (logWater)
     *   - Log bị xóa (deleteWaterLog)
     *   - Toggle Reminder được bấm
     */
    val waterUiState: StateFlow<WaterUiState> = combine(
        _selectedWaterDate,
        _isReminderEnabled
    ) { date, isReminder ->
        date to isReminder
    }.flatMapLatest { (date, isReminder) ->
            combine(
                waterRepository.observeTodaySummaryForDate(date),
                waterRepository.observeLogsForDate(date)
            ) { summary, logs ->
                val consumed = summary?.totalConsumedMl ?: 0
                val goal     = summary?.dailyGoalMl ?: 2000
                        val mappedLogs = logs.map { entity ->
                            WaterLogUiItem(
                                id        = entity.id,
                                timestamp = entity.timestamp,
                                amountMl  = entity.amountMl,
                                drinkType = entity.drinkType,
                                isDeleted = entity.isDeleted
                            )
                        }

                        // Tính toán dữ liệu biểu đồ (13 mốc từ 00:00 đến 24:00)
                        val cData = FloatArray(13) { 0f }
                        mappedLogs.forEach { log ->
                            val cal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
                            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                            val startBucket = (hour / 2) + 1
                            for (i in startBucket..12) {
                                cData[i] += log.amountMl.toFloat()
                            }
                        }

                        WaterUiState.Ready(
                            WaterScreenData(
                                consumedMl          = consumed,
                                goalMl              = goal,
                                progress            = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1f) else 0f,
                                totalCaffeineMg     = summary?.totalCaffeineMg ?: 0,
                                lastDrinkTimestamp  = summary?.lastDrinkTimestamp ?: 0L,
                                todayLogs           = mappedLogs,
                                recentLogs          = mappedLogs.sortedByDescending { it.timestamp }.take(5),
                                chartData           = cData.toList(),
                                selectedDate        = date,
                                isLogging           = false,
                                isReminderEnabled   = isReminder
                            )
                        ) as WaterUiState
            }
            .catch { e ->
                emit(WaterUiState.Error(e.message ?: "Lỗi không xác định"))
            }
        }
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000),
            initialValue  = WaterUiState.Loading
        )

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
            val sensorFlow = combine(
                stepCounterManager.todaySteps,
                stepCounterManager.calories,
                stepCounterManager.activeMinutes
            ) { steps, cal, mins -> Triple(steps, cal, mins) }

            val cloudSummaryFlow = mealRepository.observeDailySummary(today)

            // Combine sensor realtime + Room persistent + Cloud real-time
            combine(
                sensorFlow,
                healthDao.observeHealthByDate(today),
                cloudSummaryFlow,
                achievementFlow
            ) { sensorData, dbEntity, cloudSummary, achPair ->
                val (sensorSteps, sensorCal, sensorMinutes) = sensorData
                
                val baseUiState = dbEntity.toUiState(
                    sensorSteps = sensorSteps,
                    sensorCaloriesOut = sensorCal,
                    sensorActiveMinutes = sensorMinutes
                )

                // Merge cloud data for real-time dashboard update (caloriesIn, macros)
                val mergedUiState = if (cloudSummary != null) {
                    baseUiState.copy(
                        caloriesIn = (cloudSummary["caloriesIn"] as? Number)?.toInt() ?: baseUiState.caloriesIn,
                        carbs = (cloudSummary["carbs"] as? Number)?.toInt() ?: baseUiState.carbs,
                        protein = (cloudSummary["protein"] as? Number)?.toInt() ?: baseUiState.protein,
                        fat = (cloudSummary["fat"] as? Number)?.toInt() ?: baseUiState.fat
                    )
                } else {
                    baseUiState
                }

                mergedUiState.copy(
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
     * Đồng bộ thủ công với callback để hiển thị thông báo (Thực thi NGAY LẬP TỨC)
     */
    fun forceSyncWithCallback(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Ép xả bộ đệm RAM xuống Room. Lệnh true ép confirm toàn bộ bước đang có trên UI.
                stepCounterManager.flushToDatabase(true)
                
                // 2. Kéo dữ liệu từ Cloud về Local (nếu có conflict)
                healthRepository.syncCloudToLocal()
                
                // 3. Đẩy dữ liệu từ Local lên Cloud (Thực thi ngay, không qua WorkManager)
                healthRepository.pushLocalToCloud()
                
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    // ================================================================
    // WATER MODULE ACTIONS
    // ================================================================

    /**
     * Ghi nhận 1 sự kiện uống nước.
     *
     * Luồng an toàn (production-safe):
     *   1. Set isLogging = true (UI disable nút Add tránh double-tap)
     *   2. WaterRepository.logWater() chạy trên IO thread:
     *      Insert Log -> SUM -> Rebuild Summary -> Update health_history
     *   3. Flow tự emit lại -> waterUiState cập nhật -> UI render lại
     *   4. isLogging = false
     *
     * @param amountMl Lượng nước (ml). Phải > 0.
     * @param drinkType Loại thức uống. Dùng hằng số DrinkType. Mặc định WATER.
     * @param goalMl Mục tiêu nước hôm nay (ml). Lấy từ waterUiState.data.goalMl.
     * @param source Nguồn (MANUAL hoặc REMINDER). Mặc định MANUAL.
     * @param onError Callback nếu ghi thất bại (chạy trên Main thread).
     */
    fun logWater(
        amountMl: Int,
        drinkType: String = DrinkType.WATER,
        goalMl: Int = 2000,
        source: String = WaterSource.MANUAL,
        onError: ((String) -> Unit)? = null
    ) {
        if (amountMl <= 0) {
            onError?.invoke("Lượng nước phải lớn hơn 0")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                waterRepository.logWater(
                    amountMl  = amountMl,
                    drinkType = drinkType,
                    goalMl    = goalMl,
                    source    = source
                )
                
                // Nếu nhắc nhở đang bật, tự động reset lịch báo thức thêm 2 tiếng từ lúc này
                if (_isReminderEnabled.value) {
                    com.example.finfit.health.manager.WaterReminderManager.scheduleReminder(getApplication())
                }
                
                // Trigger sync ngay sau khi ghi log nước thành công
                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.finfit.health.data.HealthSyncWorker>().build()
                workManager.enqueue(syncRequest)
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError?.invoke(e.message ?: "Không thể ghi dữ liệu nước")
                }
            }
        }
    }

    /**
     * Bật/Tắt nhắc nhở uống nước
     */
    fun toggleWaterReminder(enabled: Boolean) {
        healthRepository.setWaterReminderEnabled(enabled)
        _isReminderEnabled.value = enabled
        
        if (enabled) {
            com.example.finfit.health.manager.WaterReminderManager.scheduleReminder(getApplication())
        } else {
            com.example.finfit.health.manager.WaterReminderManager.cancelReminder(getApplication())
        }
    }

    /**
     * Xóa mềm (Soft Delete) 1 log uống nước.
     * Summary sẽ tự động được Rebuild sau khi xóa.
     *
     * @param logId ID của WaterLogUiItem cần xóa.
     * @param goalMl Mục tiêu nước hôm nay (để Rebuild Summary đúng).
     */
    fun deleteWaterLog(logId: String, goalMl: Int = 2000) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                waterRepository.deleteWaterLog(logId, goalMl)
                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.finfit.health.data.HealthSyncWorker>().build()
                workManager.enqueue(syncRequest)
            } catch (e: Exception) {
                android.util.Log.e("HealthViewModel", "deleteWaterLog failed: ${e.message}", e)
            }
        }
    }

    /**
     * Đổi ngày đang xem trên WaterTrackerScreen.
     * Ngay khi gọi, _selectedWaterDate emit giá trị mới ->
     * flatMapLatest hủy subscription cũ và mở subscription mới cho ngày đó ->
     * waterUiState tự động cập nhật mà không cần gọi thêm bất kỳ hàm nào.
     *
     * @param date Ngày muốn xem ("yyyy-MM-dd").
     */
    fun changeSelectedWaterDate(date: String) {
        _selectedWaterDate.value = date
    }

    /**
     * Backward-compatible: addWater() cũ nay delegate sang logWater() mới.
     * Giữ hàm này để các màn hình cũ gọi addWater() không bị compile error.
     */
    @Deprecated(
        message = "Dùng logWater() thay thế để hỗ trợ drinkType và source",
        replaceWith = ReplaceWith("logWater(amount)")
    )
    fun addWater(amount: Int) {
        logWater(amountMl = amount)
    }

    /**
     * Thêm calo nạp vào (cho module thực phẩm sau này)
     */
    fun addCaloriesIn(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            healthRepository.updateCaloriesIn(amount)
        }
    }

    // ================================================================
    // SLEEP MODULE STATE & ACTIONS
    // ================================================================

    private val _selectedSleepDate = MutableStateFlow(getCurrentDate())

    val sleepUiState: StateFlow<SleepUiState> = _selectedSleepDate
        .flatMapLatest { date ->
            combine(
                healthDao.observeHealthByDate(date),
                sleepRepository.observeSleepSessionsForDate(date)
            ) { healthEntity, logs ->
                val totalSleepHours = healthEntity?.sleepHours ?: 0f
                val bedTimeMinute = healthRepository.getBedTimeMinute()
                val wakeTimeMinute = healthRepository.getWakeTimeMinute()

                val mappedLogs = logs.map {
                    com.example.finfit.health.model.SleepLogUiItem(
                        id = it.id,
                        bedTimeTimestamp = it.bedTimeTimestamp,
                        wakeTimeTimestamp = it.wakeTimeTimestamp,
                        sleepQuality = it.sleepQuality
                    )
                }

                SleepUiState.Ready(
                    com.example.finfit.health.model.SleepScreenData(
                        selectedDate = date,
                        totalSleepHours = totalSleepHours,
                        bedTimeMinuteOfDay = bedTimeMinute,
                        wakeTimeMinuteOfDay = wakeTimeMinute,
                        todaySessions = mappedLogs
                    )
                ) as SleepUiState
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SleepUiState.Loading
        )

    fun changeSelectedSleepDate(date: String) {
        _selectedSleepDate.value = date
    }

    fun logSleepSession(bedTime: Long, wakeTime: Long, quality: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sleepRepository.logSleepSession(
                    date = _selectedSleepDate.value,
                    bedTimeTimestamp = bedTime,
                    wakeTimeTimestamp = wakeTime,
                    sleepQuality = quality
                )
                // Trigger sync
                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.finfit.health.data.HealthSyncWorker>().build()
                workManager.enqueue(syncRequest)
            } catch (e: Exception) {
                android.util.Log.e("HealthViewModel", "logSleepSession failed: ${e.message}", e)
            }
        }
    }

    fun deleteSleepSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sleepRepository.deleteSleepSession(sessionId, _selectedSleepDate.value)
                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.finfit.health.data.HealthSyncWorker>().build()
                workManager.enqueue(syncRequest)
            } catch (e: Exception) {
                android.util.Log.e("HealthViewModel", "deleteSleepSession failed: ${e.message}", e)
            }
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
