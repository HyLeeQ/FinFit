package com.example.finfit.core.model

// ─── Loại Insight ──────────────────────────────────────────────────────────────

enum class InsightType {
    /** Chi tiêu ăn uống vs Calo nạp vào */
    FOOD_BUDGET,
    /** Vận động nhiều → tiết kiệm chi phí gym / bác sĩ */
    EXERCISE_SAVING,
    /** Ngủ ít → có xu hướng mua sắm cảm xúc tăng */
    SLEEP_PRODUCTIVITY,
    /** Điểm sức khỏe cao liên tiếp → gợi ý cộng vào tiết kiệm */
    HEALTH_SCORE_BONUS,
    /** Chi tiêu vượt ngân sách → stress tài chính */
    OVERSPEND_STRESS,
    /** Uống ít nước nhưng mua nhiều nước ngọt → lãng phí + hại sức khỏe */
    HYDRATION_SAVING,
    /** Chi phí thuốc tăng → nhắc chăm sóc sức khỏe phòng ngừa */
    MEDICAL_PREVENTION
}

// ─── Mức ưu tiên hiển thị ──────────────────────────────────────────────────────

enum class InsightPriority {
    /** Hiển thị ngay lên đầu, màu nổi bật */
    HIGH,
    /** Hiển thị bình thường */
    MEDIUM,
    /** Chỉ hiển thị khi không có HIGH/MEDIUM */
    LOW
}

// ─── Data class Insight ────────────────────────────────────────────────────────

/**
 * HealthFinanceInsight — Biểu diễn một phát hiện liên ngành Sức khỏe + Tài chính.
 *
 * @param type             Loại insight (xác định icon + màu sắc)
 * @param title            Tiêu đề ngắn gọn (~30 ký tự)
 * @param description      Mô tả chi tiết hành động gợi ý (~60 ký tự)
 * @param financialImpact  Số tiền ước tính tiết kiệm/tổn thất (+/- VND). 0 nếu không tính được.
 * @param healthImpact     Số điểm sức khỏe ước tính tăng/giảm. 0 nếu không tính được.
 * @param actionRoute      Route để navigate khi người dùng nhấn vào card. Null nếu chỉ thông báo.
 * @param priority         Mức ưu tiên hiển thị
 * @param emoji            Emoji đại diện (hiển thị thay icon trên card nhỏ)
 */
data class HealthFinanceInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val financialImpact: Double = 0.0,
    val healthImpact: Int = 0,
    val actionRoute: String? = null,
    val priority: InsightPriority = InsightPriority.MEDIUM,
    val emoji: String = "💡"
)
