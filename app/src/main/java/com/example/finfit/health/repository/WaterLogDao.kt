package com.example.finfit.health.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.finfit.health.model.WaterLogEntity
import kotlinx.coroutines.flow.Flow

// ====================================================================
// WATER LOG DAO — Thao tác với bảng water_logs (Source of Truth)
// ====================================================================

/**
 * WaterLogDao — DAO thao tác với bảng Raw Events uống nước.
 *
 * Nguyên tắc:
 * - INSERT chỉ dùng OnConflictStrategy.ABORT để phát hiện UUID trùng (không bao giờ được xảy ra).
 * - UPDATE chỉ dùng cho syncStatus và isDeleted (Soft Delete). KHÔNG bao giờ UPDATE amountMl.
 * - DELETE SQL thật chỉ dùng cho lệnh cleanup logs cũ đã SYNCED (Phase 2 sau).
 */
@Dao
interface WaterLogDao {

    // ------------------------------------------------------------------
    // WRITE OPERATIONS
    // ------------------------------------------------------------------

    /**
     * Insert 1 log uống nước mới.
     * ABORT: Nếu UUID đã tồn tại (bug) sẽ throw Exception để phát hiện sớm.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLogEntity)

    /**
     * Soft Delete: Đánh dấu log là đã xóa. KHÔNG xóa khỏi DB thật.
     * Lý do: Giữ lịch sử để analytics tính "Tỷ lệ nhập sai dữ liệu".
     */
    @Query("""
        UPDATE water_logs 
        SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 0
        WHERE id = :logId
    """)
    suspend fun softDeleteLog(logId: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * Cập nhật syncStatus theo batch (Worker sync dùng cái này).
     * Dùng IN clause để update nhiều record trong 1 lần.
     */
    @Query("UPDATE water_logs SET syncStatus = :status WHERE id IN (:logIds)")
    suspend fun updateSyncStatus(logIds: List<String>, status: Int)

    // ------------------------------------------------------------------
    // READ OPERATIONS — Cho UI (Flow để UI tự reactive)
    // ------------------------------------------------------------------

    /**
     * [PRIMARY CHART QUERY] Reactive stream của logs trong ngày để vẽ Hourly Line Chart.
     * Chỉ lấy logs chưa xóa, sắp xếp theo thời gian.
     * UI subscribe vào đây, Room tự emit lại khi có log mới.
     */
    @Query("""
        SELECT * FROM water_logs 
        WHERE date = :date AND isDeleted = 0 
        ORDER BY timestamp ASC
    """)
    fun observeLogsByDate(date: String): Flow<List<WaterLogEntity>>

    /**
     * Lấy logs trong ngày để tính SUM rebuild Summary (chạy trong Transaction).
     * Suspend (không phải Flow) vì chạy 1 lần trong Transaction, không cần reactive.
     */
    @Query("""
        SELECT * FROM water_logs 
        WHERE date = :date AND isDeleted = 0 
        ORDER BY timestamp ASC
    """)
    suspend fun getLogsByDate(date: String): List<WaterLogEntity>

    // ------------------------------------------------------------------
    // READ OPERATIONS — Cho Analytics
    // ------------------------------------------------------------------

    /**
     * [7-DAY / 30-DAY CHART] Tổng lượng nước theo từng ngày trong khoảng thời gian.
     * Kết quả là DailyWaterPojo để vẽ Bar Chart mà không load toàn bộ logs vào RAM.
     * Chú ý: startDate và endDate là string "yyyy-MM-dd", SQLite so sánh lexicographic.
     */
    @Query("""
        SELECT date, SUM(amountMl) AS totalMl
        FROM water_logs
        WHERE date BETWEEN :startDate AND :endDate AND isDeleted = 0
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyTotals(startDate: String, endDate: String): List<DailyWaterPojo>

    /**
     * [SLEEP CORRELATION] Tổng caffeine nạp vào sau 18:00 (18*3600*1000 = 64800000 ms offset).
     * Nhận vào epochStart của 18:00 hôm nay để query chính xác.
     * Dùng cho Sleep Warning: Nếu > 200mg sau 18:00 -> cảnh báo ngủ kém.
     */
    @Query("""
        SELECT SUM(caffeineMg) FROM water_logs 
        WHERE date = :date AND timestamp >= :after18hEpoch AND isDeleted = 0
    """)
    suspend fun getTotalCaffeineAfter18h(date: String, after18hEpoch: Long): Int?

    /**
     * [DRY GAP WARNING] Thời điểm uống nước gần nhất (cho WarningEngine tính DryGap).
     */
    @Query("""
        SELECT MAX(timestamp) FROM water_logs 
        WHERE date = :date AND isDeleted = 0
    """)
    suspend fun getLastDrinkTimestamp(date: String): Long?

    // ------------------------------------------------------------------
    // SYNC OPERATIONS — Cho Firebase WorkManager (Phase 2)
    // ------------------------------------------------------------------

    /**
     * Lấy tất cả logs chưa đồng bộ (syncStatus = UNSYNCED = 0).
     * WorkManager gọi hàm này để lấy batch cần push lên Firebase.
     */
    @Query("SELECT * FROM water_logs WHERE syncStatus = 0")
    suspend fun getUnsyncedLogs(): List<WaterLogEntity>

    // ------------------------------------------------------------------
    // AGGREGATE HELPERS — Dùng nội bộ trong Transaction
    // ------------------------------------------------------------------

    /** Tổng lượng nước hợp lệ trong ngày (không tính logs đã xóa) */
    @Query("SELECT SUM(amountMl) FROM water_logs WHERE date = :date AND isDeleted = 0")
    suspend fun sumAmountMlByDate(date: String): Int?

    /** Tổng caffeine trong ngày */
    @Query("SELECT SUM(caffeineMg) FROM water_logs WHERE date = :date AND isDeleted = 0")
    suspend fun sumCaffeineMgByDate(date: String): Int?

    /** MAX timestamp (lần uống cuối) trong ngày */
    @Query("SELECT MAX(timestamp) FROM water_logs WHERE date = :date AND isDeleted = 0")
    suspend fun maxTimestampByDate(date: String): Long?
}

// ------------------------------------------------------------------
// POJO — Kết quả trả về từ các query Aggregate (không phải Entity)
// ------------------------------------------------------------------

/** Kết quả truy vấn Daily Totals cho 7-day / 30-day Bar Chart */
data class DailyWaterPojo(
    val date: String,
    val totalMl: Int
)
