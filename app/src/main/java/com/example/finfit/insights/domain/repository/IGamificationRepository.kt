package com.example.finfit.insights.domain.repository

import com.example.finfit.insights.domain.model.*
import kotlinx.coroutines.flow.Flow

interface IGamificationRepository {
    fun observeProfile(userId: String): Flow<UserGamificationProfile>
    fun observeBadges(userId: String): Flow<List<GamificationBadge>>
    fun observeActiveChallenges(userId: String): Flow<List<GamificationChallenge>>
    suspend fun recordAction(userId: String, action: GamificationAction): Int
    suspend fun useStreakFreeze(userId: String): Boolean
    suspend fun claimChallengeReward(userId: String, challengeId: String)
    suspend fun setGamificationEnabled(userId: String, enabled: Boolean)
}
