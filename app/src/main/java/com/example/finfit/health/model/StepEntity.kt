package com.example.finfit.health.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_history")
data class StepEntity(
    @PrimaryKey val date: String,
    val steps: Int,
    val calories: Int = 0,
    val activeMinutes: Int = 0,
    val syncStatus: Int = 0, // 0: UNSYNCED, 1: SYNCING, 2: SYNCED
    val lastUpdated: Long = System.currentTimeMillis()
)
