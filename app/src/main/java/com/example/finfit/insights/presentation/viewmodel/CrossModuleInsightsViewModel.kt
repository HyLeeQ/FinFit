package com.example.finfit.insights.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.finfit.insights.domain.model.*
import com.example.finfit.insights.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CrossModuleInsightsUiState(
    val weeklySummary: CrossModuleWeeklySummary = CrossModuleWeeklySummary(),
    val piggybank: HealthySavingsPiggybank = HealthySavingsPiggybank(),
    val challenges: List<CrossModuleChallenge> = emptyList(),
    val badges: List<CrossModuleBadge> = emptyList(),
    val activeAlert: CrossModuleAlert? = null,
    val selectedTab: Int = 0, // 0: Tổng quan & Biểu đồ, 1: Tiết kiệm Tự Nấu, 2: Thử thách & Huy hiệu
    val isLoading: Boolean = false
)

class CrossModuleInsightsViewModel(
    private val getWeeklySummaryUseCase: GetCrossModuleWeeklySummaryUseCase,
    private val calculateHealthySavingsUseCase: CalculateHealthySavingsUseCase,
    private val getGamificationUseCase: GetCrossModuleGamificationUseCase,
    private val checkAlertUseCase: CheckCrossModuleAlertUseCase,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrossModuleInsightsUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getWeeklySummaryUseCase(userId),
                calculateHealthySavingsUseCase(userId),
                getGamificationUseCase.getChallenges(userId),
                getGamificationUseCase.getBadges(userId)
            ) { summary, piggybank, challenges, badges ->
                CrossModuleInsightsUiState(
                    weeklySummary = summary,
                    piggybank = piggybank,
                    challenges = challenges,
                    badges = badges,
                    isLoading = false
                )
            }.collect { state ->
                val alert = checkAlertUseCase(userId)
                _uiState.value = state.copy(activeAlert = alert)
            }
        }
    }
}

class CrossModuleInsightsViewModelFactory(
    private val getWeeklySummaryUseCase: GetCrossModuleWeeklySummaryUseCase,
    private val calculateHealthySavingsUseCase: CalculateHealthySavingsUseCase,
    private val getGamificationUseCase: GetCrossModuleGamificationUseCase,
    private val checkAlertUseCase: CheckCrossModuleAlertUseCase,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CrossModuleInsightsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CrossModuleInsightsViewModel(
                getWeeklySummaryUseCase,
                calculateHealthySavingsUseCase,
                getGamificationUseCase,
                checkAlertUseCase,
                userId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
