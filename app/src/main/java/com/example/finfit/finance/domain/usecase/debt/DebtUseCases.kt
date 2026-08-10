package com.example.finfit.finance.domain.usecase.debt

import com.example.finfit.finance.domain.repository.DebtLoanRepository
import com.example.finfit.finance.model.DebtLoan

class AddDebtLoanUseCase(
    private val debtLoanRepository: DebtLoanRepository
) {
    suspend operator fun invoke(userId: String, debtLoan: DebtLoan) {
        debtLoanRepository.saveDebtLoan(userId, debtLoan)
    }
}
