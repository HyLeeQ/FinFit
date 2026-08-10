package com.example.finfit.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.finfit.core.sync.IOfflineSyncManager
import com.example.finfit.core.sync.OfflineSyncManager
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.finance.data.repository.*
import com.example.finfit.finance.domain.repository.*
import com.example.finfit.finance.domain.usecase.transaction.AddTransactionUseCase
import com.example.finfit.finance.domain.usecase.transaction.GetTransactionHistoryUseCase
import com.example.finfit.finance.domain.usecase.budget.SetBudgetUseCase
import com.example.finfit.finance.domain.usecase.savings.CreateSavingsGoalUseCase
import com.example.finfit.finance.domain.usecase.savings.DepositToSavingsUseCase
import com.example.finfit.finance.domain.usecase.debt.AddDebtLoanUseCase
import com.example.finfit.finance.domain.usecase.sync.CalculateDerivedBalanceUseCase
import com.example.finfit.health.data.repository.StepRepositoryImpl
import com.example.finfit.health.data.repository.MealRepositoryImpl
import com.example.finfit.insights.data.repository.CrossModuleRepositoryImpl
import com.example.finfit.insights.data.repository.GamificationRepositoryImpl
import com.example.finfit.insights.domain.repository.ICrossModuleRepository
import com.example.finfit.insights.domain.repository.IGamificationRepository
import com.example.finfit.insights.domain.usecase.*

object ViewModelFactory {
    val transactionRepository: TransactionRepository by lazy { TransactionRepositoryImpl() }
    val budgetRepository: BudgetRepository by lazy { BudgetRepositoryImpl() }
    val savingsRepository: SavingsRepository by lazy { SavingsRepositoryImpl() }
    val debtLoanRepository: DebtLoanRepository by lazy { DebtLoanRepositoryImpl() }
    val walletRepository: WalletRepository by lazy { WalletRepositoryImpl() }
    val authRepository by lazy { AuthRepository() }

    val stepRepository by lazy { StepRepositoryImpl() }
    val mealRepository by lazy { MealRepositoryImpl() }

    val crossModuleRepository: ICrossModuleRepository by lazy {
        CrossModuleRepositoryImpl(transactionRepository, mealRepository, stepRepository)
    }

    val gamificationRepository: IGamificationRepository by lazy {
        GamificationRepositoryImpl(transactionRepository, stepRepository, mealRepository)
    }

    val offlineSyncManager: IOfflineSyncManager by lazy {
        OfflineSyncManager()
    }

    // Finance Use cases
    val addTransactionUseCase by lazy { AddTransactionUseCase(transactionRepository, walletRepository) }
    val getTransactionHistoryUseCase by lazy { GetTransactionHistoryUseCase(transactionRepository) }
    val setBudgetUseCase by lazy { SetBudgetUseCase(budgetRepository) }
    val createSavingsGoalUseCase by lazy { CreateSavingsGoalUseCase(savingsRepository) }
    val depositToSavingsUseCase by lazy { DepositToSavingsUseCase(savingsRepository, walletRepository) }
    val addDebtLoanUseCase by lazy { AddDebtLoanUseCase(debtLoanRepository) }
    val calculateDerivedBalanceUseCase by lazy { CalculateDerivedBalanceUseCase() }

    // Cross-Module Insights Use cases
    val getCrossModuleWeeklySummaryUseCase by lazy { GetCrossModuleWeeklySummaryUseCase(crossModuleRepository) }
    val calculateHealthySavingsUseCase by lazy { CalculateHealthySavingsUseCase(crossModuleRepository) }
    val getCrossModuleGamificationUseCase by lazy { GetCrossModuleGamificationUseCase(crossModuleRepository) }
    val checkCrossModuleAlertUseCase by lazy { CheckCrossModuleAlertUseCase(crossModuleRepository) }
    val linkMealToTransactionUseCase by lazy { LinkMealToTransactionUseCase(crossModuleRepository) }

    // Gamification & Motivation Use cases
    val getGamificationProfileUseCase by lazy { GetGamificationProfileUseCase(gamificationRepository) }
    val recordGamificationActionUseCase by lazy { RecordGamificationActionUseCase(gamificationRepository) }
    val useStreakFreezeUseCase by lazy { UseStreakFreezeUseCase(gamificationRepository) }
    val getGamificationBadgesUseCase by lazy { GetGamificationBadgesUseCase(gamificationRepository) }
    val getGamificationChallengesUseCase by lazy { GetGamificationChallengesUseCase(gamificationRepository) }
    val claimChallengeRewardUseCase by lazy { ClaimChallengeRewardUseCase(gamificationRepository) }
    val toggleGamificationEnabledUseCase by lazy { ToggleGamificationEnabledUseCase(gamificationRepository) }
}
