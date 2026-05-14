package com.example.finfit.health.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sleep_session_logs",
    indices = [Index(value = ["date"])]
)
data class SleepSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String, // Khóa ngoại ảo nối với HealthEntity (yyyy-MM-dd)
    val bedTimeTimestamp: Long, // Thời điểm bắt đầu ngủ (millisecond)
    val wakeTimeTimestamp: Long, // Thời điểm kết thúc ngủ (millisecond)
    val sleepQuality: Int = 3, // Điểm chất lượng giấc ngủ (1-5)
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false // Xóa mềm
)
