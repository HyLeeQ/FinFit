package com.example.finfit.health.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ====================================================================
// WATER LOG ENTITY — Bảng Log (Source of Truth tuyệt đối)
// Mọi hành động thêm nước phải đi qua đây. Không bao giờ cộng dồn
// trực tiếp vào Summary. Chỉ dùng Soft Delete (isDeleted), không DELETE thật.
// ====================================================================

/**
 * DrinkType — Chuẩn hóa loại đồ uống.
 * Dùng String thay vì Enum để Room không phải thêm TypeConverter
 * và Firebase có thể lưu trực tiếp mà không cần mapping.
 * Giá trị hợp lệ: "WATER", "COFFEE", "TEA", "MILK", "JUICE", "SODA", "OTHER"
 */
object DrinkType {
    const val WATER  = "WATER"
    const val COFFEE = "COFFEE"
    const val TEA    = "TEA"
    const val MILK   = "MILK"
    const val JUICE  = "JUICE"
    const val SODA   = "SODA"
    const val OTHER  = "OTHER"

    /** Ước lượng lượng Caffeine (mg) theo loại thức uống (phục vụ Sleep correlation sau này) */
    fun caffeineMg(drinkType: String, amountMl: Int): Int = when (drinkType) {
        COFFEE -> (amountMl * 0.36).toInt()  // ~90mg/250ml
        TEA    -> (amountMl * 0.02).toInt()  // ~20mg/250ml
        else   -> 0
    }
}

/**
 * WaterSource — Nguồn gốc hành động uống nước.
 * Dùng để phân biệt hành vi chủ động (MANUAL) và thụ động (qua Reminder).
 * Đây là tín hiệu quan trọng cho Prediction Model sau này.
 */
object WaterSource {
    const val MANUAL   = "MANUAL"    // User tự thêm
    const val REMINDER = "REMINDER"  // User thêm sau khi thấy thông báo nhắc nhở
}

/**
 * WaterSyncStatus — Trạng thái đồng bộ với Firebase.
 * Đặt tên thống nhất với SyncStatus hiện có trong HealthEntity.
 */
object WaterSyncStatus {
    const val UNSYNCED = 0
    const val SYNCING  = 1
    const val SYNCED   = 2
}

/**
 * WaterLogEntity — Bản ghi gốc cho từng lần uống nước.
 *
 * Index:
 * - [date]: Truy vấn phổ biến nhất là "Tất cả logs hôm nay" (Daily chart).
 * - [syncStatus]: Worker quét logs chưa đồng bộ cần index này để nhanh.
 *
 * Nguyên tắc Bất biến (Immutable):
 * - Không UPDATE amountMl sau khi đã lưu.
 * - Nếu user nhập sai, Soft Delete (isDeleted = true) + Insert log mới.
 * - syncStatus được phép UPDATE (0 -> 1 -> 2) theo tiến trình sync.
 */
@Entity(
    tableName = "water_logs",
    indices = [
        Index(value = ["date"]),
        Index(value = ["syncStatus"])
    ]
)
data class WaterLogEntity(
    /** UUID tạo tại Local (UUID.randomUUID().toString()). Làm Document ID trên Firebase để đảm bảo Idempotent khi Sync */
    @PrimaryKey val id: String,

    /** Ngày dạng "yyyy-MM-dd". Là Partition Key chính để query theo ngày */
    val date: String,

    /** Thời điểm uống nước chính xác (Epoch ms). Key để vẽ Hourly Chart và Time-Series */
    val timestamp: Long,

    /** Lượng nước (ml). >= 1 */
    val amountMl: Int,

    /** Loại đồ uống. Dùng hằng số trong [DrinkType] */
    val drinkType: String,

    /**
     * Caffeine ước tính (mg). Tính ngay lúc Insert bằng DrinkType.caffeineMg().
     * Lưu sẵn để tránh tính toán lặp lại khi vẽ chart Sleep-Caffeine correlation.
     */
    val caffeineMg: Int,

    /** Nguồn gốc hành động. Dùng hằng số trong [WaterSource] */
    val source: String,

    /**
     * Số bước chân đã tích lũy TẠI THỜI ĐIỂM uống nước.
     * Bắt tại lúc Insert từ StepCounterManager.todaySteps.
     * Đây là Cross-module Context Enrichment — cho phép phân tích tương quan Hydration vs Activity sau này.
     */
    val contextSteps: Int,

    /**
     * Timezone offset (giây so với UTC). Bắt tại thiết bị lúc Insert.
     * VD: UTC+7 -> timezoneOffset = 25200
     * Bảo vệ dữ liệu khi user đổi múi giờ / du lịch nước ngoài.
     */
    val timezoneOffset: Int,

    /** Soft Delete flag. TRUE khi user xóa log. KHÔNG BAO GIỜ dùng DELETE SQL thật */
    val isDeleted: Boolean = false,

    /** Epoch ms khi record được tạo trong Room */
    val createdAt: Long,

    /** Epoch ms khi record bị sửa đổi lần cuối (cập nhật isDeleted hoặc syncStatus) */
    val updatedAt: Long,

    /** Trạng thái đồng bộ. Dùng hằng số trong [WaterSyncStatus] */
    val syncStatus: Int = WaterSyncStatus.UNSYNCED
)

// ====================================================================
// WATER DAILY SUMMARY ENTITY — Bảng Cache (Read-Model)
// Không phải Source of Truth. Được tính lại từ water_logs bất cứ lúc nào.
// Mục đích duy nhất: Load Dashboard Progress Bar trong O(1), không cần SUM query.
// ====================================================================

/**
 * WaterDailySummaryEntity — Cache tổng lượng nước theo ngày.
 *
 * Luồng cập nhật (QUAN TRỌNG): Không bao giờ UPDATE bảng này trực tiếp từ UI.
 * Luồng đúng: User thêm nước -> Insert WaterLogEntity -> Room Transaction
 *              chạy SELECT SUM() từ water_logs -> Kết quả ghi vào bảng này.
 *
 * Lợi ích: Nếu bảng này bị xóa, có thể Rebuild 100% từ water_logs bất cứ lúc nào.
 */
@Entity(tableName = "water_daily_summary")
data class WaterDailySummaryEntity(
    /** Ngày "yyyy-MM-dd" (Primary Key, khớp với water_logs.date) */
    @PrimaryKey val date: String,

    /**
     * Tổng lượng nước đã uống (ml). Chỉ tính các log có isDeleted = false.
     * Được rebuild bằng: SELECT SUM(amountMl) FROM water_logs WHERE date = ? AND isDeleted = 0
     */
    val totalConsumedMl: Int,

    /**
     * Mục tiêu nước của ngày hôm đó (ml).
     * Phân biệt với waterGoal toàn cục — sau này khi có Dynamic Goal
     * (tự động tăng theo số bước), mỗi ngày có Goal riêng.
     */
    val dailyGoalMl: Int,

    /**
     * Tổng Caffeine nạp vào trong ngày (mg).
     * Rebuild bằng: SELECT SUM(caffeineMg) FROM water_logs WHERE date = ? AND isDeleted = 0.
     * Dùng cho Sleep warning nếu Caffeine > 200mg sau 18:00.
     */
    val totalCaffeineMg: Int,

    /**
     * Timestamp (Epoch ms) của lần uống nước gần nhất.
     * Lấy: SELECT MAX(timestamp) FROM water_logs WHERE date = ? AND isDeleted = 0.
     * Dùng để tính DryGap (Thời gian chưa uống nước) cho Warning System.
     */
    val lastDrinkTimestamp: Long,

    /** Epoch ms khi Summary được cập nhật lần cuối */
    val updatedAt: Long
)
