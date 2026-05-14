package com.example.finfit.health.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finfit.health.model.WaterDailySummaryEntity
import kotlinx.coroutines.flow.Flow

// ====================================================================
// WATER SUMMARY DAO — Thao tác với bảng water_daily_summary (Cache)
// ====================================================================

/**
 * WaterSummaryDao — DAO thao tác với bảng Read-Model (Summary Cache).
 *
 * LƯU Ý: Bảng này KHÔNG ĐƯỢC cập nhật trực tiếp từ UI Layer.
 * Chỉ được phép ghi từ bên trong @Transaction của WaterRepository.
 * Flow chuẩn: Insert WaterLog -> SUM từ water_logs -> upsert vào bảng này.
 */
@Dao
interface WaterSummaryDao {

    // ------------------------------------------------------------------
    // WRITE OPERATIONS
    // ------------------------------------------------------------------

    /**
     * Upsert Summary theo ngày.
     * REPLACE: Ghi đè nếu đã tồn tại (bảo đảm Summary luôn là kết quả SUM mới nhất).
     * Chỉ gọi hàm này từ bên trong WaterRepository.rebuildSummary() transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: WaterDailySummaryEntity)

    // ------------------------------------------------------------------
    // READ OPERATIONS — Cho UI (Reactive Flow)
    // ------------------------------------------------------------------

    /**
     * [PRIMARY DASHBOARD QUERY] Reactive stream của Summary hôm nay.
     * UI subscribe vào đây để vẽ Progress Bar. Room tự emit khi Summary được rebuild.
     * Dùng thay vì direct SUM query để load Dashboard tức thì (O(1) lookup).
     */
    @Query("SELECT * FROM water_daily_summary WHERE date = :date")
    fun observeSummaryByDate(date: String): Flow<WaterDailySummaryEntity?>

    /**
     * Lấy Summary của ngày cụ thể (suspend, không reactive).
     * Dùng khi cần check goalMl để init record cho ngày mới.
     */
    @Query("SELECT * FROM water_daily_summary WHERE date = :date")
    suspend fun getSummaryByDate(date: String): WaterDailySummaryEntity?

    /**
     * [7-DAY PROGRESS CHART] Lấy Summaries của nhiều ngày liên tiếp.
     * Hiệu suất tốt hơn query SUM từ water_logs vì chỉ đọc 7 records từ bảng nhỏ.
     * Thiếu ngày nào thì không có record (UI cần xử lý null = 0ml).
     */
    @Query("""
        SELECT * FROM water_daily_summary 
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
    """)
    suspend fun getSummariesInRange(startDate: String, endDate: String): List<WaterDailySummaryEntity>
}
