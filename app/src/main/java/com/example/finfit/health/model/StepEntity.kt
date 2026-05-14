package com.example.finfit.health.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * HealthEntity — Bản ghi sức khỏe tổng hợp theo ngày.
 * Lưu trữ tất cả chỉ số: bước chân, nước, calo, giấc ngủ.
 */
@Entity(tableName = "health_history")
data class HealthEntity(
    @PrimaryKey val date: String,
    val steps: Int = 0,
    val stepGoal: Int = 1000,
    val caloriesOut: Int = 0,       // Calo tiêu hao (từ vận động)
    val caloriesIn: Int = 0,        // Calo nạp vào (từ thực phẩm)
    val carbs: Int = 0,             // Tinh bột (g)
    val protein: Int = 0,           // Đạm (g)
    val fat: Int = 0,               // Chất béo (g)
    val activeMinutes: Int = 0,
    val waterConsumed: Int = 0,     // Lượng nước tiêu thụ (ml)
    val waterGoal: Int = 0,         // Mục tiêu nước trong ngày (ml)
    val sleepHours: Float = 0f,     // Số giờ ngủ
    val syncStatus: Int = 0,        // 0: UNSYNCED, 1: SYNCING, 2: SYNCED
    val lastUpdated: Long = System.currentTimeMillis()
)
