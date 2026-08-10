package com.example.finfit.finance.domain.usecase

import com.example.finfit.finance.domain.repository.IFinanceRepository
import com.example.finfit.finance.model.*

class AddTransactionUseCase(
    private val financeRepository: IFinanceRepository
) {
    suspend operator fun invoke(userId: String, transaction: FinanceTransaction, updatedWallet: AppUserWallet) {
        financeRepository.saveUserWallet(updatedWallet)
        financeRepository.addTransaction(userId, transaction)
    }
}
