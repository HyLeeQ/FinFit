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

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private var initialStepsAtStartOfDay: Int = -1

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

            CoroutineScope(Dispatchers.IO).launch {
                val date = getCurrentDate()
                val stepEntity = stepDao.getStepsByDate(date)

                // Nếu là lần đầu tiên đọc sensor trong ngày (hoặc chưa có lưu trong DB)
                if (initialStepsAtStartOfDay == -1) {
                    if (stepEntity != null && stepEntity.steps > 0) {
                        // Tính ngược lại initial load của máy nếu lỡ khởi động lại view
                        initialStepsAtStartOfDay = currentSensorSteps - stepEntity.steps
                    } else {
                        // Đây là những bước đầu tiên của ngày
                        initialStepsAtStartOfDay = currentSensorSteps
                    }
                }

                // Tính số bước hôm nay: Số hiện tại - Số ban đầu
                val stepsTodayCalculated = currentSensorSteps - initialStepsAtStartOfDay

                // Chống số âm nếu device reboot làm sensor reset về 0 (đặc trị sensor android)
                val finalSteps = if (stepsTodayCalculated < 0) {
                     initialStepsAtStartOfDay = 0 // Reset initial steps vì sensor vừa bị clear by reboot
                     currentSensorSteps
                } else {
                     stepsTodayCalculated
                }

                _todaySteps.value = finalSteps

                // Lưu vào Room
                stepDao.insertSteps(StepEntity(date = date, steps = finalSteps))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
