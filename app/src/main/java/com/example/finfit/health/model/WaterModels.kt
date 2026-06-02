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

    /**
     * Hydration Index — Hệ số cấp nước thực tế theo khuyến nghị y khoa.
     *
     * Lý do khoa học:
     * - Cà phê/Trà có tính lợi tiểu nhẹ (diuretic effect) → cơ thể bài tiết
     *   nhiều nước hơn so với lượng tiêu thụ → giá trị hydrat hóa thực thấp hơn.
     * - Sữa/Nước ép chứa điện giải & dinh dưỡng hỗ trợ giữ nước nhưng
     *   đường/chất béo làm chậm hấp thu → hệ số ~0.9.
     * - Nước ngọt có đường + gas → tính lợi tiểu nhẹ hơn cà phê → 0.75.
     *
     * @return Hệ số (0.0–1.0). VD: 0.8 nghĩa là 500ml cà phê ≈ 400ml cấp nước thực.
     */
    fun hydrationIndex(drinkType: String): Float = when (drinkType) {
        WATER  -> 1.00f   // Baseline — hoàn toàn được hấp thu
        MILK   -> 0.90f   // Giàu điện giải, hỗ trợ giữ nước nhưng có chất béo
        JUICE  -> 0.90f   // Tương tự sữa — điện giải tốt, đường làm chậm hấp thu
        TEA    -> 0.85f   // Lợi tiểu nhẹ do tanin & caffeine thấp
        COFFEE -> 0.80f   // Lợi tiểu rõ hơn do caffeine cao
        SODA   -> 0.75f   // Gas + đường + caffeine (dark soda) → kém hiệu quả nhất
        else   -> 0.90f   // OTHER: mặc định hệ số trung bình
    }

    /**
     * Tính lượng nước cấp cho cơ thể thực tế (ml) sau khi áp dụng Hydration Index.
     * Đây là giá trị dùng để cộng vào consumedMl trong Summary.
     *
     * VD: logWater(500ml, COFFEE) → effectiveHydration = 500 × 0.8 = 400ml
     */
    fun effectiveHydrationMl(amountMl: Int, drinkType: String): Int =
        (amountMl * hydrationIndex(drinkType)).toInt()

    /**
     * Caffeine (mg) — Cập nhật theo tỷ lệ chuẩn hóa y khoa:
     * - Cà phê: ~0.60mg/ml (tương đương ~90mg/150ml ly espresso)
     * - Trà: ~0.20mg/ml (tương đương ~40mg/200ml)
     */
    fun caffeineMg(drinkType: String, amountMl: Int): Int = when (drinkType) {
        COFFEE -> (amountMl * 0.60).toInt()  // ~90mg / 150ml
        TEA    -> (amountMl * 0.20).toInt()  // ~40mg / 200ml
        else   -> 0
    }

    /** Ngưỡng Caffeine cảnh báo theo khuyến nghị y khoa */
    const val CAFFEINE_WARN_MG    = 200   // Mức bắt đầu cảnh báo (màu cam)
    const val CAFFEINE_DANGER_MG  = 400   // Mức nguy hiểm (Dialog/Notification)
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

    /**
     * Lượng nước cấp cho cơ thể thực tế (ml) sau khi áp dụng Hydration Index.
     * Tính tại lúc Insert: amountMl × DrinkType.hydrationIndex(drinkType).
     * VD: 500ml Cà phê × 0.80 = 400ml effectiveHydrationMl.
     * Đây là giá trị được cộng vào consumedMl của Summary thay vì amountMl thô.
     */
    val effectiveHydrationMl: Int = amountMl,

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
