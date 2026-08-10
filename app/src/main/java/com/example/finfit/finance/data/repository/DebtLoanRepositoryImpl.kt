package com.example.finfit.finance.data.repository

import com.example.finfit.finance.domain.repository.DebtLoanRepository
import com.example.finfit.finance.model.DebtLoan
import com.example.finfit.finance.repository.FirestoreRepository
import kotlinx.coroutines.flow.Flow

class DebtLoanRepositoryImpl(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : DebtLoanRepository {

    override fun observeDebtLoans(userId: String): Flow<List<DebtLoan>> {
        return firestoreRepository.observeDebtLoans(userId)
    }

    override suspend fun saveDebtLoan(userId: String, debtLoan: DebtLoan) {
        firestoreRepository.saveDebtLoan(userId, debtLoan)
    }

    override suspend fun deleteDebtLoan(userId: String, debtLoanId: String) {
        firestoreRepository.deleteDebtLoan(userId, debtLoanId)
    }

    override suspend fun toggleDebtLoanPaidStatus(userId: String, debtLoanId: String, isPaid: Boolean) {
        firestoreRepository.toggleDebtLoanPaidStatus(userId, debtLoanId, isPaid)
    }
}
