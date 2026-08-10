package com.example.finfit.insights.domain.usecase

import com.example.finfit.insights.domain.model.*
import com.example.finfit.insights.domain.repository.IGamificationRepository
import kotlinx.coroutines.flow.Flow

class GetGamificationProfileUseCase(
    private val repository: IGamificationRepository
) {
    operator fun invoke(userId: String): Flow<UserGamificationProfile> {
        return repository.observeProfile(userId)
    }
}

class RecordGamificationActionUseCase(
    private val repository: IGamificationRepository
) {
    suspend operator fun invoke(userId: String, action: GamificationAction): Int {
        return repository.recordAction(userId, action)
    }
}

class UseStreakFreezeUseCase(
    private val repository: IGamificationRepository
) {
    suspend operator fun invoke(userId: String): Boolean {
        return repository.useStreakFreeze(userId)
    }
}

class GetGamificationBadgesUseCase(
    private val repository: IGamificationRepository
) {
    operator fun invoke(userId: String): Flow<List<GamificationBadge>> {
        return repository.observeBadges(userId)
    }
}

class GetGamificationChallengesUseCase(
    private val repository: IGamificationRepository
) {
    operator fun invoke(userId: String): Flow<List<GamificationChallenge>> {
        return repository.observeActiveChallenges(userId)
    }
}

class ClaimChallengeRewardUseCase(
    private val repository: IGamificationRepository
) {
    suspend operator fun invoke(userId: String, challengeId: String) {
        repository.claimChallengeReward(userId, challengeId)
    }
}

class ToggleGamificationEnabledUseCase(
    private val repository: IGamificationRepository
) {
    suspend operator fun invoke(userId: String, enabled: Boolean) {
        repository.setGamificationEnabled(userId, enabled)
    }
}
