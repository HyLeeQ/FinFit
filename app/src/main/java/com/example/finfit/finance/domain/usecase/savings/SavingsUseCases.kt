package com.example.finfit.finance.domain.usecase.savings

import com.example.finfit.finance.domain.repository.SavingsRepository
import com.example.finfit.finance.domain.repository.WalletRepository
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.model.SavingsGoal

class CreateSavingsGoalUseCase(
    private val savingsRepository: SavingsRepository
) {
    suspend operator fun invoke(userId: String, goal: SavingsGoal) {
        savingsRepository.saveSavingsGoal(userId, goal)
    }
}

class DepositToSavingsUseCase(
    private val savingsRepository: SavingsRepository,
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(
        userId: String,
        goal: SavingsGoal,
        depositAmount: Double,
        updatedWallet: AppUserWallet
    ) {
        val updatedGoal = goal.copy(currentAmount = goal.currentAmount + depositAmount)
        savingsRepository.saveSavingsGoal(userId, updatedGoal)
        walletRepository.saveUserWallet(updatedWallet)
    }
}
