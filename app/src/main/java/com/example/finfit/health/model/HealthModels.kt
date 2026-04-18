package com.example.finfit.health.model

import androidx.compose.ui.graphics.vector.ImageVector

// ====================================================================
// HEALTH SCREEN STATE — Sealed class cho UI State Management
// ====================================================================

/**
 * HealthScreenState — Quản lý trạng thái màn hình Sức khỏe.
 * Cho phép UI phân biệt rõ ràng giữa Loading/Success/Error.
 */
sealed class HealthScreenState {
    /** Đang tải dữ liệu */
    data object Loading : HealthScreenState()

    /** Tải thành công — chứa dữ liệu UI */
    data class Success(val data: HealthUiState) : HealthScreenState()

    /** Có lỗi xảy ra */
    data class Error(val message: String) : HealthScreenState()
}

// ====================================================================
// HEALTH UI STATE — Data class tổng hợp cho giao diện
// ====================================================================

/**
 * HealthUiState — Trạng thái UI tổng hợp cho toàn bộ Health module.
 * Đã tách ra khỏi ViewModel để quản lý tập trung tại tầng Model.
 */
data class HealthUiState(
    val steps: Int = 0,
    val stepGoal: Int = 1000,
    val caloriesOut: Int = 0,
    val caloriesIn: Int = 0,
    val activeMinutes: Int = 0,
    val activeMinuteGoal: Int = 60,
    val waterConsumedMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val sleepHours: Float = 0f,
    val isFirst1000StepsAchieved: Boolean = false,
    val hasCelebrated1000Steps: Boolean = false
) {
    /** Calo thuần: Nạp vào - Tiêu hao */
    val netCalorieBalance: Int get() = caloriesIn - caloriesOut
}

// ====================================================================
// WATER PRESET — Định nghĩa loại đồ uống
// ====================================================================

/**
 * WaterPreset — Định nghĩa thông tin preset cho loại đồ uống.
 * Sử dụng trong Dialog thêm nước.
 */
data class WaterPreset(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val defaultAmount: Int   // Dung tích mặc định (ml)
)

// ====================================================================
// EXTENSION FUNCTIONS — Chuyển đổi Entity ↔ UiState
// ====================================================================

/**
 * Chuyển đổi HealthEntity (Room DB) sang HealthUiState (UI).
 * Kết hợp dữ liệu sensor realtime với persistent DB để lấy giá trị cao nhất.
 *
 * @param sensorSteps Bước chân realtime từ sensor
 * @param sensorCaloriesOut Calo tiêu hao realtime
 * @param sensorActiveMinutes Phút vận động realtime
 */
fun HealthEntity?.toUiState(
    sensorSteps: Int = 0,
    sensorCaloriesOut: Int = 0,
    sensorActiveMinutes: Int = 0
): HealthUiState {
    if (this == null) {
        return HealthUiState(
            steps = sensorSteps,
            caloriesOut = sensorCaloriesOut,
            activeMinutes = sensorActiveMinutes
        )
    }
    
    val finalWaterGoal = if (waterGoal > 0) waterGoal else 2000

    return HealthUiState(
        steps = maxOf(sensorSteps, steps),
        stepGoal = stepGoal,
        caloriesOut = maxOf(sensorCaloriesOut, caloriesOut),
        caloriesIn = caloriesIn,
        activeMinutes = maxOf(sensorActiveMinutes, activeMinutes),
        activeMinuteGoal = 60,
        waterConsumedMl = waterConsumed,
        waterGoalMl = finalWaterGoal,
        sleepHours = sleepHours
    )
}
