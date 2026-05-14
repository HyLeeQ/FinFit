package com.example.finfit.health.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finfit.health.model.HealthEntity
import kotlinx.coroutines.flow.Flow

/**
 * HealthDao — Giao tiếp trực tiếp với bảng health_history.
 * Mọi thao tác CRUD sức khỏe đều thông qua đây.
 */
@Dao
interface HealthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealth(entity: HealthEntity)

    @Query("SELECT * FROM health_history WHERE date = :date")
    suspend fun getHealthByDate(date: String): HealthEntity?

    /** Reactive query — Room tự emit mỗi khi bảng health_history thay đổi */
    @Query("SELECT * FROM health_history WHERE date = :date")
    fun observeHealthByDate(date: String): Flow<HealthEntity?>

    @Query("SELECT * FROM health_history WHERE syncStatus != 2")
    suspend fun getUnsyncedRecords(): List<HealthEntity>

    @Query("UPDATE health_history SET syncStatus = :status WHERE date = :date")
    suspend fun updateSyncStatus(date: String, status: Int)

    /** Partial Update: Chỉ cập nhật nước, không đụng vào steps/calories */
    @Query("UPDATE health_history SET waterConsumed = waterConsumed + :addedWater, waterGoal = :goal, syncStatus = 0 WHERE date = :date")
    suspend fun updateWaterConsumption(date: String, addedWater: Int, goal: Int)

    /**
     * Direct SET waterConsumed (thay thế delta-based updateWaterConsumption).
     * Dùng bởi WaterRepository để sync ngược health_history sau khi rebuild từ SUM.
     * Tuyệt đối an toàn — không bao giờ gây cộng dồn trùng lặp.
     */
    @Query("UPDATE health_history SET waterConsumed = :totalMl, waterGoal = :goal, syncStatus = 0 WHERE date = :date")
    suspend fun setWaterConsumed(date: String, totalMl: Int, goal: Int)

    /** Partial Update: Chỉ cập nhật caloriesIn cho module thực phẩm */
    @Query("UPDATE health_history SET caloriesIn = caloriesIn + :amount, syncStatus = 0 WHERE date = :date")
    suspend fun updateCaloriesIn(date: String, amount: Int)

    /** Partial Update: Cập nhật dinh dưỡng tổng hợp */
    @Query("UPDATE health_history SET carbs = carbs + :carbs, protein = protein + :protein, fat = fat + :fat, caloriesIn = caloriesIn + :calories, syncStatus = 0 WHERE date = :date")
    suspend fun updateMacros(date: String, carbs: Int, protein: Int, fat: Int, calories: Int)

    /** Lấy dữ liệu theo dải ngày cho biểu đồ */
    @Query("SELECT * FROM health_history WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getHealthHistoryRange(startDate: String, endDate: String): List<HealthEntity>

    @Query("SELECT * FROM health_history WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeHealthHistoryRange(startDate: String, endDate: String): Flow<List<HealthEntity>>

    /** Partial Update: Chỉ cập nhật step data, KHÔNG đụng vào water/caloriesIn/sleep. Dùng MAX() để không bao giờ ghi đè giá trị cao hơn */
    @Query("UPDATE health_history SET steps = MAX(steps, :steps), caloriesOut = MAX(caloriesOut, :caloriesOut), activeMinutes = MAX(activeMinutes, :activeMinutes), syncStatus = 0, lastUpdated = :lastUpdated WHERE date = :date")
    suspend fun updateStepData(date: String, steps: Int, caloriesOut: Int, activeMinutes: Int, lastUpdated: Long)

    /** Reset chỉ bước chân trong ngày, KHÔNG đụng nước/calo/sleep */
    @Query("UPDATE health_history SET steps = 0, caloriesOut = 0, activeMinutes = 0, carbs = 0, protein = 0, fat = 0, caloriesIn = 0, syncStatus = 0, lastUpdated = :lastUpdated WHERE date = :date")
    suspend fun resetStepData(date: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("DELETE FROM health_history")
    suspend fun deleteAll()
}
