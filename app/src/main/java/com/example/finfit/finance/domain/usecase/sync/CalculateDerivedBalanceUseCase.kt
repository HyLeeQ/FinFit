package com.example.finfit.finance.domain.usecase.sync

import com.example.finfit.finance.model.AppBankAccount
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.model.TransactionType

class CalculateDerivedBalanceUseCase {

    data class DerivedAccountBalance(
        val accountId: String,
        val accountName: String,
        val calculatedBalance: Double,
        val totalIncome: Double,
        val totalExpense: Double
    )

    data class DerivedWalletSummary(
        val totalBalance: Double,
        val totalSpendable: Double,
        val accounts: List<DerivedAccountBalance>
    )

    operator fun invoke(
        wallet: AppUserWallet?,
        transactions: List<FinanceTransaction>
    ): DerivedWalletSummary {
        if (wallet == null) {
            return DerivedWalletSummary(0.0, 0.0, emptyList())
        }

        val accountMap = wallet.accounts.associateBy { it.id }.toMutableMap()
        val incomeMap = mutableMapOf<String, Double>()
        val expenseMap = mutableMapOf<String, Double>()
        val balanceDeltaMap = mutableMapOf<String, Double>()

        wallet.accounts.forEach { acc ->
            incomeMap[acc.id] = 0.0
            expenseMap[acc.id] = 0.0
            balanceDeltaMap[acc.id] = 0.0
        }

        transactions.forEach { tx ->
            val srcId = tx.accountId
            val dstId = tx.toAccountId

            when (tx.type) {
                TransactionType.INCOME -> {
                    if (srcId != null) {
                        incomeMap[srcId] = (incomeMap[srcId] ?: 0.0) + tx.amount
                        balanceDeltaMap[srcId] = (balanceDeltaMap[srcId] ?: 0.0) + tx.amount
                    }
                }
                TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> {
                    if (srcId != null) {
                        expenseMap[srcId] = (expenseMap[srcId] ?: 0.0) + tx.amount
                        balanceDeltaMap[srcId] = (balanceDeltaMap[srcId] ?: 0.0) - tx.amount
                    }
                }
                TransactionType.TRANSFER -> {
                    if (srcId != null) {
                        balanceDeltaMap[srcId] = (balanceDeltaMap[srcId] ?: 0.0) - tx.amount
                    }
                    if (dstId != null) {
                        balanceDeltaMap[dstId] = (balanceDeltaMap[dstId] ?: 0.0) + tx.amount
                    }
                }
            }
        }

        val derivedAccounts = wallet.accounts.map { acc ->
            val delta = balanceDeltaMap[acc.id] ?: 0.0
            val calculated = acc.amount + delta
            DerivedAccountBalance(
                accountId = acc.id,
                accountName = acc.name,
                calculatedBalance = calculated,
                totalIncome = incomeMap[acc.id] ?: 0.0,
                totalExpense = expenseMap[acc.id] ?: 0.0
            )
        }

        val totalCalculated = derivedAccounts.sumOf { it.calculatedBalance }
        val spendable = (totalCalculated - (wallet.generalSavings)).coerceAtLeast(0.0)

        return DerivedWalletSummary(
            totalBalance = totalCalculated,
            totalSpendable = spendable,
            accounts = derivedAccounts
        )
    }
}
