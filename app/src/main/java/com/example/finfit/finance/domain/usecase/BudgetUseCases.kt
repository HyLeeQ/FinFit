package com.example.finfit.finance.domain.usecase

import com.example.finfit.finance.model.FinanceBudget
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.ui.logic.BudgetLogic
import com.example.finfit.finance.ui.logic.BudgetPaceResult

class CalculateBudgetPaceUseCase {
    operator fun invoke(budget: FinanceBudget, transactions: List<FinanceTransaction>): BudgetPaceResult {
        return BudgetLogic.calculateSpendingPace(budget, transactions)
    }
}

class SuggestBudgetUseCase {
    operator fun invoke(category: String, transactions: List<FinanceTransaction>): Double {
        return BudgetLogic.suggestBudgetAmount(category, transactions)
    }
}
