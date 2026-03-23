package com.example.finfit.health.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.finfit.health.model.StepEntity
import com.google.android.gms.location.ActivityRecognition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class StepCounterManager private constructor(private val context: Context) : SensorEventListener {

    companion object {
        @Volatile
        private var INSTANCE: StepCounterManager? = null

        fun getInstance(context: Context): StepCounterManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepCounterManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private const val DB_WRITE_STEP_THRESHOLD = 10
        private const val DB_WRITE_TIME_THRESHOLD = 30_000L
        private const val ACCEL_SHAKE_THRESHOLD = 20.0
        private const val GYRO_SHAKE_THRESHOLD = 5.0
        private const val AI_TIMEOUT_MS = 60_000L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Dual Sensor Strategy:
    //   STEP_COUNTER → nguồn chính xác (cumulative, nhưng batch 8-10 bước/lần)
    //   STEP_DETECTOR → nguồn mượt (1 event mỗi bước, dùng cho UI)
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    // Anti-Cheat Sensors
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val stepDao = HealthDatabase.getDatabase(context).stepDao()
    private val prefs = context.getSharedPreferences("StepTrackerPrefs", Context.MODE_PRIVATE)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _calories = MutableStateFlow(0)
    val calories: StateFlow<Int> = _calories.asStateFlow()

    private val _activeMinutes = MutableStateFlow(0)
    val activeMinutes: StateFlow<Int> = _activeMinutes.asStateFlow()

    // ====== State: Active Time Tracking ======
    private var activeTimeAccumulatedMs: Long = 0L
    private var lastActiveCheckTimeMs: Long = 0L
    private var isListening: Boolean = false

    // ====== State: Step Tracker (STEP_COUNTER) ======
    private var inMemorySavedDate: String = ""
    private var inMemorySensorBaseline: Int = -1
    private var inMemoryAccumulatedSteps: Int = 0
    private var inMemoryLastSensorValue: Int = -1

    // ====== State: Smooth UI Bridge (STEP_DETECTOR ↔ STEP_COUNTER) ======
    // lastConfirmedSteps = số chính xác từ STEP_COUNTER lần cuối
    // pendingDetectorSteps = bước tạm từ STEP_DETECTOR chưa được confirm
    // Khi STEP_COUNTER cập nhật → lastConfirmedSteps = giá trị mới, pendingDetectorSteps reset = 0
    private var lastConfirmedSteps: Int = 0
    private var pendingDetectorSteps: Int = 0

    // ====== State: Anti-Cheat ======
    private var latestAccelMagnitude: Double = 9.8
    private var latestGyroMagnitude: Double = 0.0
    private var isShaking: Boolean = false
    private var shakeEndTimeMillis: Long = 0L

    // ====== State: Buffered DB Write ======
    private var lastDbWriteTimeMillis: Long = 0L
    private var stepsSinceLastDbWrite: Int = 0

    init {
        val date = getCurrentDate()
        inMemorySavedDate = prefs.getString("saved_date", date) ?: date
        inMemorySensorBaseline = prefs.getInt("sensor_baseline", -1)
        inMemoryAccumulatedSteps = prefs.getInt("accumulated_steps", 0)
        inMemoryLastSensorValue = prefs.getInt("last_sensor_value", -1)
        activeTimeAccumulatedMs = prefs.getLong("active_time_ms", 0L)
        lastDbWriteTimeMillis = System.currentTimeMillis()
        lastActiveCheckTimeMs = System.currentTimeMillis()

        if (inMemorySavedDate != date) {
            inMemorySavedDate = date
            inMemorySensorBaseline = -1
            inMemoryAccumulatedSteps = 0
            inMemoryLastSensorValue = -1
            _todaySteps.value = 0
            _calories.value = 0
            _activeMinutes.value = 0
            activeTimeAccumulatedMs = 0L
            lastConfirmedSteps = 0
            saveStateToPrefs()
        } else {
            _activeMinutes.value = (activeTimeAccumulatedMs / 60_000L).toInt()
            loadStepsFromDb()
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadStepsFromDb() {
        CoroutineScope(Dispatchers.IO).launch {
            val date = getCurrentDate()
            val stepEntity = stepDao.getStepsByDate(date)
            val steps = stepEntity?.steps ?: 0
            _todaySteps.value = steps
            lastConfirmedSteps = steps
        }
    }

    fun startListening() {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST, 0)
        }
        stepDetectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST, 0)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        try {
            ActivityRecognition.getClient(context)
                .requestActivityUpdates(5000L, pendingIntent)
        } catch (_: SecurityException) { }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)

        val intent = Intent(context, ActivityTransitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        try {
            ActivityRecognition.getClient(context).removeActivityUpdates(pendingIntent)
        } catch (_: SecurityException) { }

        CoroutineScope(Dispatchers.IO).launch {
            flushToDatabase()
        }
    }

    // ====== SENSOR EVENT HANDLER ======
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val now = System.currentTimeMillis()

        if (isShaking && now > shakeEndTimeMillis) {
            isShaking = false
        }

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                latestAccelMagnitude = sqrt((x * x + y * y + z * z).toDouble())
                if (latestAccelMagnitude > ACCEL_SHAKE_THRESHOLD) {
                    isShaking = true
                    shakeEndTimeMillis = now + 2000L
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                latestGyroMagnitude = sqrt((x * x + y * y + z * z).toDouble())
                if (latestGyroMagnitude > GYRO_SHAKE_THRESHOLD) {
                    isShaking = true
                    shakeEndTimeMillis = now + 2000L
                }
            }

            // ─── STEP_DETECTOR: UI mượt, nhảy +1 mỗi bước ───
            // Hiển thị tạm: lastConfirmedSteps + pendingDetectorSteps
            // Khi STEP_COUNTER bắt kịp → recalibrate, pending reset
            Sensor.TYPE_STEP_DETECTOR -> {
                if (isStepAllowed()) {
                    pendingDetectorSteps++
                    _todaySteps.value = lastConfirmedSteps + pendingDetectorSteps
                }
            }

            // ─── STEP_COUNTER: Nguồn chính xác, recalibrate UI ───
            Sensor.TYPE_STEP_COUNTER -> {
                processStepEvent(event.values[0].toInt(), now)
            }
        }
    }

    // ====== BỘ LỌC 3 LỚP ======
    private fun isStepAllowed(): Boolean {
        if (isShaking) return false

        val isUserWalking = prefs.getBoolean("isUserWalking", true)
        if (!isUserWalking) {
            val lastUpdate = prefs.getLong("lastActivityUpdateTime", 0L)
            val timeSinceUpdate = System.currentTimeMillis() - lastUpdate
            if (timeSinceUpdate > AI_TIMEOUT_MS) return true
            return false
        }

        return true
    }

    // ====== XỬ LÝ STEP_COUNTER EVENT ======
    private fun processStepEvent(currentSensorSteps: Int, currentTime: Long) {
        if (currentSensorSteps <= 0) return

        val currentDate = getCurrentDate()

        // ─── Đổi ngày ───
        if (inMemorySavedDate != currentDate) {
            inMemorySavedDate = currentDate
            inMemorySensorBaseline = currentSensorSteps
            inMemoryAccumulatedSteps = 0
            inMemoryLastSensorValue = currentSensorSteps
            lastConfirmedSteps = 0
            pendingDetectorSteps = 0
            _todaySteps.value = 0
            saveStateToPrefs()
        }

        // ─── Khởi tạo baseline lần đầu ───
        if (inMemorySensorBaseline == -1 || inMemoryLastSensorValue == -1) {
            inMemorySensorBaseline = currentSensorSteps
            inMemoryLastSensorValue = currentSensorSteps
            saveStateToPrefs()
            return
        }

        // ─── Phát hiện Reboot ───
        if (currentSensorSteps < inMemoryLastSensorValue) {
            val stepsBeforeReboot = inMemoryLastSensorValue - inMemorySensorBaseline
            if (stepsBeforeReboot > 0) {
                inMemoryAccumulatedSteps += stepsBeforeReboot
            }
            inMemorySensorBaseline = currentSensorSteps
            inMemoryLastSensorValue = currentSensorSteps
            saveStateToPrefs()
            return
        }

        // ─── Anti-Cheat Gate ───
        if (!isStepAllowed()) {
            val rejectedSteps = currentSensorSteps - inMemoryLastSensorValue
            if (rejectedSteps > 0) {
                inMemorySensorBaseline += rejectedSteps
            }
            inMemoryLastSensorValue = currentSensorSteps
            saveStateToPrefs()
            return
        }

        // ─── Tính bước chính xác ───
        inMemoryLastSensorValue = currentSensorSteps
        val todayStepsCalculated = inMemoryAccumulatedSteps + (currentSensorSteps - inMemorySensorBaseline)

        if (todayStepsCalculated < 0) return

        // ──── RECALIBRATE: Đồng bộ STEP_DETECTOR với STEP_COUNTER ────
        // STEP_COUNTER là nguồn chính xác → ghi đè giá trị, reset pending
        lastConfirmedSteps = todayStepsCalculated
        pendingDetectorSteps = 0
        _todaySteps.value = todayStepsCalculated
        _calories.value = (todayStepsCalculated * 0.04).toInt()

        // Tích lũy Active Time khi isUserWalking == true
        val now2 = System.currentTimeMillis()
        val isUserWalking = prefs.getBoolean("isUserWalking", true)
        if (isUserWalking && lastActiveCheckTimeMs > 0L) {
            val delta = now2 - lastActiveCheckTimeMs
            if (delta in 1..10_000L) { // chỉ tích lũy delta hợp lý (< 10s)
                activeTimeAccumulatedMs += delta
                _activeMinutes.value = (activeTimeAccumulatedMs / 60_000L).toInt()
            }
        }
        lastActiveCheckTimeMs = now2

        saveStateToPrefs()

        // Buffered ghi Room DB
        stepsSinceLastDbWrite++
        val timeSinceLastWrite = currentTime - lastDbWriteTimeMillis
        if (stepsSinceLastDbWrite >= DB_WRITE_STEP_THRESHOLD || timeSinceLastWrite >= DB_WRITE_TIME_THRESHOLD) {
            CoroutineScope(Dispatchers.IO).launch {
                stepDao.insertSteps(StepEntity(
                    date = currentDate,
                    steps = todayStepsCalculated,
                    calories = _calories.value,
                    activeMinutes = _activeMinutes.value,
                    syncStatus = 0,
                    lastUpdated = System.currentTimeMillis()
                ))
            }
            stepsSinceLastDbWrite = 0
            lastDbWriteTimeMillis = currentTime
        }
    }

    private fun saveStateToPrefs() {
        prefs.edit()
            .putString("saved_date", inMemorySavedDate)
            .putInt("sensor_baseline", inMemorySensorBaseline)
            .putInt("accumulated_steps", inMemoryAccumulatedSteps)
            .putInt("last_sensor_value", inMemoryLastSensorValue)
            .putLong("active_time_ms", activeTimeAccumulatedMs)
            .apply()
    }

    suspend fun flushToDatabase() {
        val currentSteps = _todaySteps.value
        if (currentSteps > 0) {
            val snapDate = getCurrentDate()
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                stepDao.insertSteps(StepEntity(
                    date = snapDate,
                    steps = currentSteps,
                    calories = _calories.value,
                    activeMinutes = _activeMinutes.value,
                    syncStatus = 0,
                    lastUpdated = System.currentTimeMillis()
                ))
            }
            stepsSinceLastDbWrite = 0
            lastDbWriteTimeMillis = System.currentTimeMillis()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
}
