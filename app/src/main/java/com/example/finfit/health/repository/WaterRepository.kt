package com.example.finfit.health.repository

import android.content.Context
import android.util.Log
import com.example.finfit.health.model.WaterDailySummaryEntity
import com.example.finfit.health.model.WaterLogEntity
import com.example.finfit.health.model.DrinkType
import com.example.finfit.health.model.WaterSource
import com.example.finfit.health.model.WaterSyncStatus
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * WaterRepository — Lớp nghiệp vụ duy nhất cho toàn bộ Water Module.
 *
 * Luồng ghi DUY NHẤT được phép (CQRS Write-side):
 *   UI -> logWater() -> [Insert WaterLog] -> [Rebuild Summary from SUM] -> Done
 *
 * KHÔNG cho phép bất kỳ logic nào ở UI/ViewModel cập nhật bảng water_daily_summary trực tiếp.
 */
class WaterRepository(
    context: Context,
    private val todayStepsProvider: () -> Int = { 0 }
) {

    private val db = HealthDatabase.getDatabase(context)
    private val waterLogDao = db.waterLogDao()
    private val waterSummaryDao = db.waterSummaryDao()
    private val healthDao = db.healthDao()

    // ------------------------------------------------------------------
    // PUBLIC API — Dùng cho ViewModel
    // ------------------------------------------------------------------

    /**
     * Ghi nhận 1 sự kiện uống nước.
     *
     * Luồng Transaction an toàn:
     *   1. Sinh UUID mới -> Insert vào water_logs
     *   2. SELECT SUM(amountMl) lại từ DB (chống Race Condition nếu bấm nhiều lần)
     *   3. Upsert kết quả SUM vào water_daily_summary
     *   4. Đồng thời Partial Update health_history.waterConsumed để HealthDashboard
     *      không cần thay đổi (Backward Compatibility)
     *
     * @param amountMl Lượng nước (ml). Phải > 0.
     * @param drinkType Loại đồ uống. Dùng hằng số [DrinkType].
     * @param goalMl Mục tiêu nước của ngày hôm nay (ml). Tính toán từ Firestore user weight.
     * @param source Nguồn hành động. Mặc định là MANUAL.
     */
    suspend fun logWater(
        amountMl: Int,
        drinkType: String = DrinkType.WATER,
        goalMl: Int = 2000,
        source: String = WaterSource.MANUAL
    ) {
        require(amountMl > 0) { "amountMl phải lớn hơn 0" }

        val now = System.currentTimeMillis()
        val date = todayDateString()
        val timezoneOffsetSeconds = TimeZone.getDefault().getOffset(now) / 1000
        val caffeine = DrinkType.caffeineMg(drinkType, amountMl)
        // Tính lượng nước cấp thực tế theo Hydration Index y học
        val effectiveHydration = DrinkType.effectiveHydrationMl(amountMl, drinkType)
        val currentSteps = todayStepsProvider()

        val newLog = WaterLogEntity(
            id                   = UUID.randomUUID().toString(),
            date                 = date,
            timestamp            = now,
            amountMl             = amountMl,
            drinkType            = drinkType,
            caffeineMg           = caffeine,
            effectiveHydrationMl = effectiveHydration,
            source               = source,
            contextSteps         = currentSteps,
            timezoneOffset       = timezoneOffsetSeconds,
            isDeleted            = false,
            createdAt            = now,
            updatedAt            = now,
            syncStatus           = WaterSyncStatus.UNSYNCED
        )

        try {
            // Step 1: Insert Log vào bảng water_logs
            waterLogDao.insertLog(newLog)
            Log.d("WaterAudit", "[1/3] INSERT water_logs OK | id=${newLog.id} | +${amountMl}ml $drinkType (effective=${effectiveHydration}ml) | date=$date")

            // Step 2 + 3: Rebuild Summary từ SUM (chống Race Condition)
            rebuildSummaryForDate(date, goalMl)

            // Step 4: Sync ngược health_history bằng SET trực tiếp (dùng effective ml)
            val totalEffectiveMl = waterLogDao.sumEffectiveHydrationMlByDate(date) ?: 0
            ensureHealthEntityExists(date, goalMl)
            healthDao.setWaterConsumed(date, totalEffectiveMl, goalMl)
            Log.d("WaterAudit", "[3/3] SET health_history.waterConsumed=${totalEffectiveMl}ml (effective) | date=$date")
        } catch (e: Exception) {
            Log.e("WaterRepository", "logWater FAILED: ${e.message}", e)
            throw e
        }
    }

    /**
     * Soft Delete một log uống nước (User nhập sai và muốn xóa).
     * Sau khi Soft Delete xong, tự động Rebuild Summary.
     *
     * @param logId ID của WaterLogEntity cần xóa.
     * @param goalMl Mục tiêu nước hôm nay (để Rebuild Summary đúng).
     */
    suspend fun deleteWaterLog(logId: String, goalMl: Int = 2000) {
        val date = todayDateString()
        waterLogDao.softDeleteLog(logId)
        // Rebuild Summary trước — dùng effectiveHydrationMl (Hydration Index)
        // để totalConsumedMl và totalCaffeineMg trong Summary đều chính xác ngay lập tức.
        // Đây là điều kiện để Marquee Ticker hạ màu cảnh báo xuống sau khi user xóa log.
        rebuildSummaryForDate(date, goalMl)
        // Sync ngược health_history bằng effective ml (khớp với Summary)
        val totalEffectiveMl = waterLogDao.sumEffectiveHydrationMlByDate(date) ?: 0
        healthDao.setWaterConsumed(date, totalEffectiveMl, goalMl)
        Log.d("WaterRepository", "deleteWaterLog OK: logId=$logId, newEffectiveTotal=${totalEffectiveMl}ml")
    }

    // ------------------------------------------------------------------
    // REACTIVE STREAMS — ViewModel subscribe để render UI
    // ------------------------------------------------------------------

    /**
     * Stream reactive của Daily Summary hôm nay cho Dashboard Progress Bar.
     * Emit lại mỗi khi logWater() hoặc deleteWaterLog() cập nhật bảng summary.
     */
    fun observeTodaySummary(): Flow<WaterDailySummaryEntity?> {
        return waterSummaryDao.observeSummaryByDate(todayDateString())
    }

    /**
     * Stream reactive của Daily Summary theo ngày cụ thể.
     * HealthViewModel dùng cái này với flatMapLatest để switch date.
     */
    fun observeTodaySummaryForDate(date: String): Flow<WaterDailySummaryEntity?> {
        return waterSummaryDao.observeSummaryByDate(date)
    }

    /**
     * Stream reactive của danh sách Logs hôm nay để vẽ Hourly Bezier Line Chart.
     * Emit lại mỗi khi log mới được Insert.
     */
    fun observeTodayLogs(): Flow<List<WaterLogEntity>> {
        return waterLogDao.observeLogsByDate(todayDateString())
    }

    /**
     * Stream reactive của danh sách Logs theo ngày cụ thể.
     * HealthViewModel dùng cái này với flatMapLatest để switch date.
     */
    fun observeLogsForDate(date: String): Flow<List<WaterLogEntity>> {
        return waterLogDao.observeLogsByDate(date)
    }

    // ------------------------------------------------------------------
    // ANALYTICS QUERIES — ViewModel dùng để lấy data cho chart
    // ------------------------------------------------------------------

    /**
     * Lấy dữ liệu tổng lượng nước của nhiều ngày (cho 7-day / 30-day Bar Chart).
     * @param startDate Ngày bắt đầu "yyyy-MM-dd"
     * @param endDate Ngày kết thúc "yyyy-MM-dd"
     */
    suspend fun getDailyTotals(startDate: String, endDate: String): List<DailyWaterPojo> {
        return waterLogDao.getDailyTotals(startDate, endDate)
    }

    /**
     * Lấy Summaries nhiều ngày từ cache (nhanh hơn getDailyTotals khi chỉ cần totalMl + goal).
     */
    suspend fun getSummariesInRange(startDate: String, endDate: String): List<WaterDailySummaryEntity> {
        return waterSummaryDao.getSummariesInRange(startDate, endDate)
    }

    // ------------------------------------------------------------------
    // INTERNAL HELPERS
    // ------------------------------------------------------------------

    /**
     * Rebuild Summary từ SUM query (Source of Truth là water_logs).
     * Gọi sau mỗi Insert và Soft Delete để bảo đảm Summary luôn chính xác.
     * Đây là trái tim của CQRS Write-side.
     */
    suspend fun rebuildSummaryForDate(date: String, goalMl: Int) {
        // Dùng effectiveHydrationMl thay amountMl thô — áp dụng Hydration Index
        val totalEffectiveMl = waterLogDao.sumEffectiveHydrationMlByDate(date) ?: 0
        val totalCaffeine = waterLogDao.sumCaffeineMgByDate(date) ?: 0
        val lastDrinkTs = waterLogDao.maxTimestampByDate(date) ?: 0L

        waterSummaryDao.upsertSummary(
            WaterDailySummaryEntity(
                date               = date,
                totalConsumedMl    = totalEffectiveMl,
                dailyGoalMl        = goalMl,
                totalCaffeineMg    = totalCaffeine,
                lastDrinkTimestamp = lastDrinkTs,
                updatedAt          = System.currentTimeMillis()
            )
        )
        Log.d("WaterAudit", "[2/3] UPSERT water_daily_summary OK | effectiveTotal=${totalEffectiveMl}ml, caffeine=${totalCaffeine}mg | date=$date")
    }

    /**
     * Migration Legacy: Chạy 1 lần duy nhất sau khi DB nâng version lên 6.
     * Nếu health_history có waterConsumed > 0 mà water_logs cho ngày đó còn rỗng,
     * sinh 1 dummy log "MIGRATION" để bảo toàn số liệu cũ.
     *
     * Gọi từ Application.onCreate() hoặc HealthViewModel.init().
     */
    suspend fun migrateLegacyWaterData() {
        val date = todayDateString()
        val existingLogs = waterLogDao.getLogsByDate(date)
        if (existingLogs.isNotEmpty()) return // Đã có data mới, không cần migrate

        val healthEntity = healthDao.getHealthByDate(date) ?: return
        if (healthEntity.waterConsumed <= 0) return

        val now = System.currentTimeMillis()
        val migrationLog = WaterLogEntity(
            id             = UUID.randomUUID().toString(),
            date           = date,
            timestamp      = now,
            amountMl       = healthEntity.waterConsumed,
            drinkType      = DrinkType.WATER,
            caffeineMg     = 0,
            source         = "MIGRATION_LEGACY",
            contextSteps   = 0,
            timezoneOffset = TimeZone.getDefault().getOffset(now) / 1000,
            isDeleted      = false,
            createdAt      = now,
            updatedAt      = now,
            syncStatus     = WaterSyncStatus.UNSYNCED
        )
        waterLogDao.insertLog(migrationLog)
        rebuildSummaryForDate(date, maxOf(healthEntity.waterGoal, 2000))
        Log.d("WaterRepository", "Migration Legacy: ${healthEntity.waterConsumed}ml migrated for $date")
    }

    // ------------------------------------------------------------------
    // PRIVATE UTILITIES
    // ------------------------------------------------------------------

    private fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /**
     * Đảm bảo health_history có record cho hôm nay trước khi gọi Partial Update.
     * Tránh lỗi: SET không insert được record mới (no-op nếu chưa có row).
     */
    private suspend fun ensureHealthEntityExists(date: String, goalMl: Int) {
        if (healthDao.getHealthByDate(date) == null) {
            healthDao.insertHealth(
                com.example.finfit.health.model.HealthEntity(
                    date        = date,
                    waterGoal   = goalMl,
                    syncStatus  = 0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }
}
