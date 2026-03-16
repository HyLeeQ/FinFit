package com.example.finfit.health.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_history")
data class StepEntity(
    @PrimaryKey val date: String,
    val steps: Int
)
