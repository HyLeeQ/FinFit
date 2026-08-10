package com.example.finfit.insights.data.repository

import com.example.finfit.finance.domain.repository.TransactionRepository
import com.example.finfit.health.domain.repository.MealRepository
import com.example.finfit.health.domain.repository.StepRepository
import com.example.finfit.insights.domain.model.*
import com.example.finfit.insights.domain.repository.IGamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar

class GamificationRepositoryImpl(
    private val transactionRepository: TransactionRepository,
    private val stepRepository: StepRepository,
    private val mealRepository: MealRepository
) : IGamificationRepository {

    private val _bonusXp = MutableStateFlow(1250) // Default starting XP for user progress demo
    private val _freezesRemaining = MutableStateFlow(2)
    private val _isFrozenToday = MutableStateFlow(false)
    private val _isGamificationEnabled = MutableStateFlow(true)
    private val _completedChallengeIds = MutableStateFlow(setOf("challenge_no_coffee"))

    override fun observeProfile(userId: String): Flow<UserGamificationProfile> {
        return combine(
            transactionRepository.observeTransactions(userId, 200),
            _bonusXp.asStateFlow(),
            _freezesRemaining.asStateFlow(),
            _isFrozenToday.asStateFlow(),
            _isGamificationEnabled.asStateFlow()
        ) { txs, bonusXp, freezes, isFrozen, enabled ->
            val totalXp = bonusXp + (txs.size * 20)
            val currentTier = LevelTier.fromXp(totalXp)
            val xpInTier = totalXp - currentTier.minXp
            val tierRange = (currentTier.maxXp - currentTier.minXp).coerceAtLeast(1)
            val progressRatio = (xpInTier.toFloat() / tierRange).coerceIn(0f, 1f)
            val remainingToNext = (currentTier.maxXp - totalXp).coerceAtLeast(0)

            // Compute logging streak
            val loggingStreak = computeLoggingStreak(txs).coerceAtLeast(5)

            val streakData = StreakData(
                loggingStreak = loggingStreak,
                budgetStreakWeeks = 3,
                healthStreak = 4,
                doubleSynergyStreak = 3,
                freezesRemaining = freezes,
                isFrozenToday = isFrozen
            )

            UserGamificationProfile(
                currentXp = totalXp,
                levelTier = currentTier,
                nextLevelRemainingXp = remainingToNext,
                levelProgressRatio = progressRatio,
                streakData = streakData,
                unlockedBadgeCount = 5,
                totalBadgeCount = 10,
                anonymousDisciplineBenchmark = 78, // Top 22%
                isGamificationEnabled = enabled
            )
        }
    }

    override fun observeBadges(userId: String): Flow<List<GamificationBadge>> {
        return flowOf(
            listOf(
                // 1. Kỷ Luật
                GamificationBadge(
                    id = "badge_budget_keeper",
                    title = "Người Giữ Ngân Sách",
                    description = "Duy trì 4 tuần liên tiếp không vượt hạn mức chi tiêu",
                    icon = "🛡️",
                    category = BadgeCategory.DISCIPLINE,
                    xpReward = 150,
                    isUnlocked = true,
                    unlockedDateStr = "3 tuần trước",
                    progressRatio = 1.0f,
                    currentProgressText = "4/4 tuần"
                ),
                GamificationBadge(
                    id = "badge_trusted_debtor",
                    title = "Chủ Nợ / Con Nợ Uy Tín",
                    description = "Thanh toán hoặc tất toán nợ đúng hẹn 5 lần",
                    icon = "💳",
                    category = BadgeCategory.DISCIPLINE,
                    xpReward = 100,
                    isUnlocked = true,
                    unlockedDateStr = "Hôm qua",
                    progressRatio = 1.0f,
                    currentProgressText = "5/5 lần"
                ),

                // 2. Tăng Trưởng
                GamificationBadge(
                    id = "badge_first_goal",
                    title = "Viên Gạch Đầu Tiên",
                    description = "Hoàn thành 100% mục tiêu tiết kiệm đầu tiên",
                    icon = "🧱",
                    category = BadgeCategory.GROWTH,
                    xpReward = 200,
                    isUnlocked = true,
                    unlockedDateStr = "Tuần trước",
                    progressRatio = 1.0f,
                    currentProgressText = "100% hoàn thành"
                ),
                GamificationBadge(
                    id = "badge_positive_networth",
                    title = "Tài Sản Ròng Dương",
                    description = "Tổng số dư tài khoản lớn hơn toàn bộ nợ phải trả",
                    icon = "📈",
                    category = BadgeCategory.GROWTH,
                    xpReward = 150,
                    isUnlocked = true,
                    unlockedDateStr = "Tháng này",
                    progressRatio = 1.0f,
                    currentProgressText = "Số dư > Nợ"
                ),

                // 3. Thói Quen Tốt
                GamificationBadge(
                    id = "badge_chef_master",
                    title = "Bếp Trưởng Tiết Kiệm",
                    description = "Tự nấu ăn tại nhà ít nhất 7 ngày trong 2 tuần",
                    icon = "👨‍🍳",
                    category = BadgeCategory.HEALTHY_HABIT,
                    xpReward = 120,
                    isUnlocked = true,
                    unlockedDateStr = "2 ngày trước",
                    progressRatio = 1.0f,
                    currentProgressText = "7/7 ngày"
                ),
                GamificationBadge(
                    id = "badge_calm_weekend",
                    title = "Cuối Tuần Thanh Tịnh",
                    description = "Không phát sinh chi tiêu mua sắm ngẫu hứng vào Thứ 7 - Chủ Nhật",
                    icon = "🌿",
                    category = BadgeCategory.HEALTHY_HABIT,
                    xpReward = 100,
                    isUnlocked = false,
                    progressRatio = 0.5f,
                    currentProgressText = "1/2 ngày cuối tuần"
                ),

                // 4. Khám Phá
                GamificationBadge(
                    id = "badge_ocr_master",
                    title = "Mắt Thần OCR",
                    description = "Quét hóa đơn chi tiêu thành công lần đầu tiên",
                    icon = "📸",
                    category = BadgeCategory.EXPLORATION,
                    xpReward = 50,
                    isUnlocked = true,
                    unlockedDateStr = "Đã đạt",
                    progressRatio = 1.0f,
                    currentProgressText = "Đã trải nghiệm"
                ),
                GamificationBadge(
                    id = "badge_split_leader",
                    title = "Thủ Lĩnh Chia Tiền",
                    description = "Sử dụng tính năng chia bill Split Bill cho nhóm bạn",
                    icon = "👥",
                    category = BadgeCategory.EXPLORATION,
                    xpReward = 80,
                    isUnlocked = false,
                    progressRatio = 0.0f,
                    currentProgressText = "Chưa thử"
                ),
                GamificationBadge(
                    id = "badge_fitie_buddy",
                    title = "Bạn Đồng Hành Fitie",
                    description = "Trò chuyện và nhận phân tích tài chính từ Fitie AI",
                    icon = "✨",
                    category = BadgeCategory.EXPLORATION,
                    xpReward = 50,
                    isUnlocked = true,
                    unlockedDateStr = "Hôm nay",
                    progressRatio = 1.0f,
                    currentProgressText = "Đã hoàn thành"
                )
            )
        )
    }

    override fun observeActiveChallenges(userId: String): Flow<List<GamificationChallenge>> {
        return flowOf(
            listOf(
                GamificationChallenge(
                    id = "challenge_no_coffee",
                    title = "7 Ngày No-Coffee Shop",
                    description = "Tự pha cà phê tại nhà, không mua cà phê ngoài tiệm. Tiết kiệm ước tính ~200k!",
                    icon = "☕🏠",
                    xpReward = 150,
                    targetCount = 7,
                    currentCount = 5,
                    deadlineDaysLeft = 2,
                    isCompleted = false,
                    rewardBadgeTitle = "Huy hiệu Bậc Thầy Cà Phê Nhà"
                ),
                GamificationChallenge(
                    id = "challenge_step_and_save",
                    title = "Chiến Dịch Fit & Rich",
                    description = "Đạt trung bình 8.000 bước/ngày và giữ chi tiêu ăn uống dưới 60k/ngày",
                    icon = "🏃‍♂️💰",
                    xpReward = 200,
                    targetCount = 7,
                    currentCount = 6,
                    deadlineDaysLeft = 1,
                    isCompleted = false,
                    rewardBadgeTitle = "Huy hiệu Chiến Binh Fit & Rich"
                ),
                GamificationChallenge(
                    id = "challenge_seasonal_holiday",
                    title = "Tiết Kiệm Mùa Lễ Hội",
                    description = "Không phát sinh chi tiêu vượt ngân sách quà tặng & giải trí",
                    icon = "🎁🛡️",
                    xpReward = 300,
                    targetCount = 14,
                    currentCount = 14,
                    deadlineDaysLeft = 0,
                    isCompleted = true,
                    rewardBadgeTitle = "Huy hiệu Kỷ Luật Vàng"
                )
            )
        )
    }

    override suspend fun recordAction(userId: String, action: GamificationAction): Int {
        _bonusXp.value += action.xpValue
        return action.xpValue
    }

    override suspend fun useStreakFreeze(userId: String): Boolean {
        if (_freezesRemaining.value > 0 && !_isFrozenToday.value) {
            _freezesRemaining.value -= 1
            _isFrozenToday.value = true
            return true
        }
        return false
    }

    override suspend fun claimChallengeReward(userId: String, challengeId: String) {
        _completedChallengeIds.value = _completedChallengeIds.value + challengeId
        _bonusXp.value += 150
    }

    override suspend fun setGamificationEnabled(userId: String, enabled: Boolean) {
        _isGamificationEnabled.value = enabled
    }

    private fun computeLoggingStreak(txs: List<com.example.finfit.finance.model.FinanceTransaction>): Int {
        if (txs.isEmpty()) return 0
        val tempCal = Calendar.getInstance()
        val distinctDays = txs.map {
            tempCal.time = it.timestamp.toDate()
            "${tempCal.get(Calendar.YEAR)}_${tempCal.get(Calendar.DAY_OF_YEAR)}"
        }.distinct().size
        return distinctDays.coerceAtMost(30)
    }
}
