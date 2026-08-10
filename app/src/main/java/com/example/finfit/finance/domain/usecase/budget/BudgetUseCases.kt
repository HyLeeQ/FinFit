package com.example.finfit.finance.domain.usecase.budget

import com.example.finfit.finance.domain.repository.BudgetRepository
import com.example.finfit.finance.model.FinanceBudget
import com.example.finfit.finance.model.FinanceTransaction

class SetBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(userId: String, budget: FinanceBudget) {
        budgetRepository.saveBudget(userId, budget)
    }
}

class CheckBudgetAlertUseCase {
    operator fun invoke(budget: FinanceBudget, spentAmount: Double): Boolean {
        return spentAmount >= (budget.amount * 0.9)
    }
}
