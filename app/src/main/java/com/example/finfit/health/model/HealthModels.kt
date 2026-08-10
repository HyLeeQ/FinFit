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
    val calorieGoal: Int = 2200,
    val carbs: Int = 0,
    val carbsGoal: Int = 250,
    val protein: Int = 0,
    val proteinGoal: Int = 120,
    val fat: Int = 0,
    val fatGoal: Int = 70,
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

    // --- DAILY HEALTH SCORE LOGIC (Tự động reset qua ngày mới vì data trong ngày sẽ = 0) ---
    val nutritionScore: Int get() = if (calorieGoal > 0) ((caloriesIn.toFloat() / calorieGoal) * 30f).coerceIn(0f, 30f).toInt() else 0
    val waterScore: Int get() = if (waterGoalMl > 0) ((waterConsumedMl.toFloat() / waterGoalMl) * 20f).coerceIn(0f, 20f).toInt() else 0
    val sleepScore: Int get() = ((sleepHours / 8f) * 25f).coerceIn(0f, 25f).toInt()
    val activityScore: Int get() = if (stepGoal > 0) ((steps.toFloat() / stepGoal) * 25f).coerceIn(0f, 25f).toInt() else 0
    
    val totalHealthScore: Int get() = nutritionScore + waterScore + sleepScore + activityScore
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
// WATER UI STATE — State riêng cho WaterTrackerScreen
// ====================================================================

/**
 * WaterUiState — Sealed class quản lý trạng thái màn hình Uống nước.
 *
 * Tách biệt hoàn toàn khỏi HealthUiState để:
 *  - WaterTrackerScreen chỉ subscribe vào state của riêng nó (không re-render vì bước chân thay đổi)
 *  - Dễ dàng mở rộng Water module mà không làm ảnh hưởng Dashboard tổng
 */
sealed class WaterUiState {
    /** Lần đầu load, chưa có data */
    data object Loading : WaterUiState()

    /** Đã có data — chứa toàn bộ thông tin cần thiết cho UI */
    data class Ready(val data: WaterScreenData) : WaterUiState()

    /** Có lỗi nghiệp vụ (VD: Log thất bại) */
    data class Error(val message: String) : WaterUiState()
}

/**
 * WaterScreenData — Data class tổng hợp mọi thông tin UI cần hiển thị.
 *
 * Thiết kế: Không để lộ Entity/DAO ra ngoài ViewModel.
 * UI chỉ đọc data class này, không nhận WaterLogEntity trực tiếp.
 */
data class WaterScreenData(
    /** Lượng nước đã uống hôm nay (ml) — từ WaterDailySummary.totalConsumedMl */
    val consumedMl: Int = 0,

    /** Mục tiêu nước hôm nay (ml) — từ WaterDailySummary.dailyGoalMl */
    val goalMl: Int = 2000,

    /** Tiến độ hoàn thành (0f -> 1f). Tính sẵn để UI không phải tính toán */
    val progress: Float = 0f,

    /** Tổng Caffeine nạp vào hôm nay (mg) — từ WaterDailySummary.totalCaffeineMg */
    val totalCaffeineMg: Int = 0,

    /** Timestamp (Epoch ms) lần uống nước gần nhất — từ WaterDailySummary.lastDrinkTimestamp */
    val lastDrinkTimestamp: Long = 0L,

    /** Danh sách raw logs để vẽ Hourly Chart — từ WaterLogDao.observeLogsByDate() */
    val todayLogs: List<WaterLogUiItem> = emptyList(),

    /** Projection dùng riêng cho UI danh sách: 5 logs gần nhất, sắp xếp mới nhất lên đầu */
    val recentLogs: List<WaterLogUiItem> = emptyList(),

    /** Dữ liệu biểu đồ tích lũy (13 điểm: 00:00 đến 24:00 cách nhau 2 tiếng) */
    val chartData: List<Float> = emptyList(),

    /** Ngày đang xem (yyyy-MM-dd). Mặc định là hôm nay */
    val selectedDate: String = "",

    /**
     * True trong khoảng thời gian gọi logWater() đang thực thi (cho Progress Indicator nhỏ).
     * Không block cả màn hình, chỉ disable nút Add để tránh bấm liên tục.
     */
    val isLogging: Boolean = false,

    /** Trạng thái bật tắt nhắc nhở uống nước */
    val isReminderEnabled: Boolean = false
) {
    /** Số ml còn thiếu để đạt goal */
    val remainingMl: Int get() = maxOf(0, goalMl - consumedMl)
}

/**
 * WaterLogUiItem — Đại diện của 1 sự kiện uống nước trong UI.
 * Chuyển từ WaterLogEntity sang, không bao giờ expose Entity ra View.
 */
data class WaterLogUiItem(
    val id: String,
    val timestamp: Long,
    val amountMl: Int,
    val drinkType: String,
    val isDeleted: Boolean
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
    sensorActiveMinutes: Int = 0,
    calorieGoal: Int = 2200,
    carbsGoal: Int = 250,
    proteinGoal: Int = 120,
    fatGoal: Int = 70,
    waterGoalMl: Int = 2000
): HealthUiState {
    if (this == null) {
        return HealthUiState(
            steps = sensorSteps,
            caloriesOut = sensorCaloriesOut,
            activeMinutes = sensorActiveMinutes,
            calorieGoal = calorieGoal,
            carbsGoal = carbsGoal,
            proteinGoal = proteinGoal,
            fatGoal = fatGoal,
            waterGoalMl = waterGoalMl
        )
    }
    
    val finalWaterGoal = if (waterGoal > 0) waterGoal else waterGoalMl

    return HealthUiState(
        steps = maxOf(sensorSteps, steps),
        stepGoal = stepGoal,
        caloriesOut = maxOf(sensorCaloriesOut, caloriesOut),
        caloriesIn = caloriesIn,
        calorieGoal = calorieGoal,
        carbs = carbs,
        carbsGoal = carbsGoal,
        protein = protein,
        proteinGoal = proteinGoal,
        fat = fat,
        fatGoal = fatGoal,
        activeMinutes = maxOf(sensorActiveMinutes, activeMinutes),
        activeMinuteGoal = 60,
        waterConsumedMl = waterConsumed,
        waterGoalMl = finalWaterGoal,
        sleepHours = sleepHours
    )
}

// ====================================================================
// SLEEP MODULE STATE
// ====================================================================

/**
 * SleepUiState — Sealed class cho trạng thái UI của SleepTrackerScreen.
 */
sealed class SleepUiState {
    data object Loading : SleepUiState()
    data class Ready(val data: SleepScreenData) : SleepUiState()
    data class Error(val message: String) : SleepUiState()
}

/**
 * SleepScreenData — Chứa toàn bộ dữ liệu hiển thị trên màn hình Giấc ngủ.
 */
data class SleepScreenData(
    val selectedDate: String = "",
    val totalSleepHours: Float = 0f,
    val sleepGoalHours: Float = 8f,
    val bedTimeMinuteOfDay: Int = 22 * 60, // Lấy từ preferences
    val wakeTimeMinuteOfDay: Int = 8 * 60, // Lấy từ preferences
    val todaySessions: List<SleepLogUiItem> = emptyList()
) {
    val progress: Float get() = if (sleepGoalHours > 0f) (totalSleepHours / sleepGoalHours).coerceIn(0f, 1f) else 0f
}

/**
 * SleepLogUiItem — Đại diện cho 1 giấc ngủ trên UI.
 */
data class SleepLogUiItem(
    val id: String,
    val bedTimeTimestamp: Long,
    val wakeTimeTimestamp: Long,
    val sleepQuality: Int
) {
    val durationHours: Float get() = (wakeTimeTimestamp - bedTimeTimestamp) / (1000f * 60 * 60)
}
