package com.example.finfit.finance.domain.repository

import com.example.finfit.finance.model.DebtLoan
import kotlinx.coroutines.flow.Flow

interface DebtLoanRepository {
    fun observeDebtLoans(userId: String): Flow<List<DebtLoan>>
    suspend fun saveDebtLoan(userId: String, debtLoan: DebtLoan)
    suspend fun deleteDebtLoan(userId: String, debtLoanId: String)
    suspend fun toggleDebtLoanPaidStatus(userId: String, debtLoanId: String, isPaid: Boolean)
}
