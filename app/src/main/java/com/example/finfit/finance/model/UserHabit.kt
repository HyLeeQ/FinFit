package com.example.finfit.finance.model

// ──────────────────────────────────────────────────────────────
//  Thói quen & Lịch trình thông minh (AI Persona)
// ──────────────────────────────────────────────────────────────
data class RoutineSchedule(
    val startDay: Int = 1, // 1: Thứ 2
    val endDay: Int = 3,   // 3: Thứ 4
    val location: String = "Trọ", // "Trọ" hoặc "Nhà"
    val note: String = ""
)

data class UserHabit(
    val minMealCost: Double = 0.0,
    val maxMealCost: Double = 0.0,
    val routineSchedules: List<RoutineSchedule> = emptyList(),
    val fixedCosts: List<SpendingScheduleItem> = emptyList(),
    val lastProactiveWeek: String = "", // Định dạng "yyyy-ww" để kiểm tra đã hỏi trong tuần chưa
    val generalNotes: String = "" // "ở nhà = không tốn tiền ăn", v.v.
)
