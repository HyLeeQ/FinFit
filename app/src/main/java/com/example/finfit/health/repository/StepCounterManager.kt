package com.example.finfit.health.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.finfit.health.model.StepEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepCounterManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val stepDao = HealthDatabase.getDatabase(context).stepDao()
    private val prefs = context.getSharedPreferences("StepTrackerPrefs", Context.MODE_PRIVATE)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    init {
        loadStepsFromDb()
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun loadStepsFromDb() {
        CoroutineScope(Dispatchers.IO).launch {
            val date = getCurrentDate()
            val stepEntity = stepDao.getStepsByDate(date)
            if (stepEntity != null) {
                // Tách biệt Room DB để người dùng thấy giá trị ngay lập tức khi mở app
                _todaySteps.value = stepEntity.steps
            } else {
                _todaySteps.value = 0
            }
        }
    }

    fun startListening() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val currentSensorSteps = event.values[0].toInt()

            // Bỏ qua giá trị rác 0 của Android Sensor đôi khi gửi ngay lập tức sau khi đăng ký
            if (currentSensorSteps <= 0) return

            CoroutineScope(Dispatchers.IO).launch {
                val currentDate = getCurrentDate()
                
                var savedDate = prefs.getString("saved_date", "")
                var sensorBaseline = prefs.getInt("sensor_baseline", currentSensorSteps)
                var accumulatedSteps = prefs.getInt("accumulated_steps", 0)
                var lastSensorValue = prefs.getInt("last_sensor_value", currentSensorSteps)

                // Phát hiện sang ngày mới
                if (savedDate != currentDate) {
                    savedDate = currentDate
                    sensorBaseline = currentSensorSteps
                    accumulatedSteps = 0
                    lastSensorValue = currentSensorSteps
                }

                // Phát hiện Reboot (Sensor bị reset vòng đời)
                if (currentSensorSteps < lastSensorValue) {
                    val stepsBeforeReboot = lastSensorValue - sensorBaseline
                    if (stepsBeforeReboot > 0) {
                        accumulatedSteps += stepsBeforeReboot
                    }
                    sensorBaseline = 0
                }

                lastSensorValue = currentSensorSteps

                // Tính toán số bước chân thuần tuý hôm nay
                val todayStepsCalculated = accumulatedSteps + (currentSensorSteps - sensorBaseline)

                // Cập nhật State liên tục cho lần sau
                prefs.edit()
                    .putString("saved_date", savedDate)
                    .putInt("sensor_baseline", sensorBaseline)
                    .putInt("accumulated_steps", accumulatedSteps)
                    .putInt("last_sensor_value", lastSensorValue)
                    .apply()

                // Bắn Data lên Giao diện
                _todaySteps.value = todayStepsCalculated

                // Lưu vào Room DB làm Lịch Sử
                stepDao.insertSteps(StepEntity(currentDate, todayStepsCalculated))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
