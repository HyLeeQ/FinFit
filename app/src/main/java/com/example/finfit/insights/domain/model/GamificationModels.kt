package com.example.finfit.insights.domain.model

enum class BadgeCategory(val title: String, val icon: String) {
    DISCIPLINE("Kỷ Luật", "🛡️"),
    GROWTH("Tăng Trưởng", "📈"),
    HEALTHY_HABIT("Thói Quen Tốt", "🥗"),
    EXPLORATION("Khám Phá", "🔍")
}

data class LevelTier(
    val levelNumber: Int,
    val title: String,
    val minXp: Int,
    val maxXp: Int,
    val badgeIcon: String,
    val levelColorHex: Long
) {
    companion object {
        val ALL_TIERS = listOf(
            LevelTier(1, "Tập Sự Khởi Đầu", 0, 500, "🥉", 0xFFCD7F32),
            LevelTier(2, "Người Quản Lý Thông Minh", 501, 1500, "🥈", 0xFFC0C0C0),
            LevelTier(3, "Chuyên Gia Kỷ Luật", 1501, 3500, "🥇", 0xFFFFD700),
            LevelTier(4, "Bậc Thầy Cân Bằng", 3501, 7000, "💎", 0xFF64B5F6),
            LevelTier(5, "Huyền Thoại FinFit", 7001, 15000, "👑", 0xFF81C784)
        )

        fun fromXp(xp: Int): LevelTier {
            return ALL_TIERS.findLast { xp >= it.minXp } ?: ALL_TIERS.first()
        }
    }
}

data class StreakData(
    val loggingStreak: Int = 0,         // Chuỗi ngày ghi chép liên tiếp
    val budgetStreakWeeks: Int = 0,     // Chuỗi tuần tuân thủ ngân sách
    val healthStreak: Int = 0,          // Chuỗi ngày đạt mục tiêu sức khỏe
    val doubleSynergyStreak: Int = 0,   // Chuỗi ngày hoàn thành cả Tài chính & Sức khỏe
    val freezesRemaining: Int = 2,      // Số lượt đóng băng chuỗi còn lại trong tháng
    val isFrozenToday: Boolean = false  // Hôm nay có đang được bảo vệ bởi Freeze không
)

data class GamificationBadge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: BadgeCategory,
    val xpReward: Int,
    val isUnlocked: Boolean,
    val unlockedDateStr: String? = null,
    val progressRatio: Float = 0f,      // 0.0f - 1.0f
    val currentProgressText: String = ""
)

data class GamificationChallenge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val xpReward: Int,
    val targetCount: Int,
    val currentCount: Int,
    val deadlineDaysLeft: Int,
    val isCompleted: Boolean,
    val rewardBadgeTitle: String? = null
)

data class UserGamificationProfile(
    val currentXp: Int = 0,
    val levelTier: LevelTier = LevelTier.ALL_TIERS.first(),
    val nextLevelRemainingXp: Int = 500,
    val levelProgressRatio: Float = 0f,
    val streakData: StreakData = StreakData(),
    val unlockedBadgeCount: Int = 0,
    val totalBadgeCount: Int = 12,
    val anonymousDisciplineBenchmark: Int = 80, // Top 20% (vượt qua 80% người dùng)
    val isGamificationEnabled: Boolean = true
)

enum class GamificationAction(val xpValue: Int, val description: String) {
    LOG_TRANSACTION(20, "Ghi chép giao dịch hàng ngày"),
    KEEP_BUDGET_WEEK(100, "Tuân thủ ngân sách tuần an toàn"),
    HIT_DAILY_STEPS(30, "Đạt mục tiêu bước chân hôm nay"),
    HIT_DAILY_WATER(20, "Uống đủ nước hôm nay"),
    HOME_COOKED_MEAL(25, "Tự nấu ăn tại nhà"),
    FIRST_OCR_SCAN(50, "Sử dụng tính năng quét hóa đơn"),
    FIRST_SPLIT_BILL(50, "Chia tiền nhóm Split Bill"),
    FIRST_AI_CONSULTATION(50, "Trò chuyện & tư vấn cùng Fitie AI"),
    STREAK_MILESTONE(150, "Đạt cột mốc chuỗi 7 ngày liên tiếp")
}
