package com.example.finfit.finance.domain.usecase

import com.example.finfit.finance.model.*
import com.example.finfit.finance.util.FinancialHealthCalculator
import com.example.finfit.finance.util.FinancialHealthResult

class CalculateFinancialHealthUseCase {
    operator fun invoke(
        wallet: AppUserWallet?,
        transactions: List<FinanceTransaction>,
        budgets: List<FinanceBudget>,
        goals: List<SavingsGoal>,
        debtLoans: List<DebtLoan>
    ): FinancialHealthResult {
        return FinancialHealthCalculator.calculate(
            wallet = wallet,
            transactions = transactions,
            budgets = budgets,
            goals = goals,
            debtLoans = debtLoans
        )
    }
}
