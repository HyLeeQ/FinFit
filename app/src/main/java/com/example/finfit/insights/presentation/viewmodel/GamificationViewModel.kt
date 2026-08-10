package com.example.finfit.insights.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finfit.insights.domain.model.*
import com.example.finfit.insights.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GamificationUiState(
    val profile: UserGamificationProfile = UserGamificationProfile(),
    val badges: List<GamificationBadge> = emptyList(),
    val challenges: List<GamificationChallenge> = emptyList(),
    val isLoading: Boolean = false,
    val toastMessage: String? = null
)

class GamificationViewModel(
    private val getProfileUseCase: GetGamificationProfileUseCase,
    private val getBadgesUseCase: GetGamificationBadgesUseCase,
    private val getChallengesUseCase: GetGamificationChallengesUseCase,
    private val useStreakFreezeUseCase: UseStreakFreezeUseCase,
    private val claimRewardUseCase: ClaimChallengeRewardUseCase,
    private val toggleEnabledUseCase: ToggleGamificationEnabledUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState(isLoading = true))
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    fun loadData(userId: String) {
        viewModelScope.launch {
            combine(
                getProfileUseCase(userId),
                getBadgesUseCase(userId),
                getChallengesUseCase(userId)
            ) { profile, badges, challenges ->
                GamificationUiState(
                    profile = profile,
                    badges = badges,
                    challenges = challenges,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun useStreakFreeze(userId: String) {
        viewModelScope.launch {
            val success = useStreakFreezeUseCase(userId)
            if (success) {
                _uiState.update { it.copy(toastMessage = "❄️ Đã kích hoạt Freeze! Chuỗi của bạn hôm nay được an toàn.") }
            } else {
                _uiState.update { it.copy(toastMessage = "Bạn đã hết lượt Freeze trong tháng hoặc chuỗi hôm nay đã được bảo vệ.") }
            }
        }
    }

    fun claimChallenge(userId: String, challengeId: String) {
        viewModelScope.launch {
            claimRewardUseCase(userId, challengeId)
            _uiState.update { it.copy(toastMessage = "🎉 Chúc mừng bạn đã nhận thưởng thử thách thành công! (+150 XP)") }
        }
    }

    fun toggleGamification(userId: String, enabled: Boolean) {
        viewModelScope.launch {
            toggleEnabledUseCase(userId, enabled)
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}

class GamificationViewModelFactory(
    private val getProfileUseCase: GetGamificationProfileUseCase,
    private val getBadgesUseCase: GetGamificationBadgesUseCase,
    private val getChallengesUseCase: GetGamificationChallengesUseCase,
    private val useStreakFreezeUseCase: UseStreakFreezeUseCase,
    private val claimRewardUseCase: ClaimChallengeRewardUseCase,
    private val toggleEnabledUseCase: ToggleGamificationEnabledUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GamificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GamificationViewModel(
                getProfileUseCase,
                getBadgesUseCase,
                getChallengesUseCase,
                useStreakFreezeUseCase,
                claimRewardUseCase,
                toggleEnabledUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
