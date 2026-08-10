package com.example.finfit.finance.domain.usecase.transaction

import com.example.finfit.finance.domain.repository.TransactionRepository
import com.example.finfit.finance.domain.repository.WalletRepository
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.util.SmartTransactionParser
import com.example.finfit.finance.util.ParsedTransaction
import kotlinx.coroutines.flow.Flow

class AddTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(userId: String, transaction: FinanceTransaction, updatedWallet: AppUserWallet) {
        walletRepository.saveUserWallet(updatedWallet)
        transactionRepository.addTransaction(userId, transaction)
    }
}

class GetTransactionHistoryUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(userId: String, limit: Long = 50): Flow<List<FinanceTransaction>> {
        return transactionRepository.observeTransactions(userId, limit)
    }
}

class ParseTransactionFromTextUseCase {
    operator fun invoke(text: String, userId: String = ""): ParsedTransaction? {
        return SmartTransactionParser.parse(text)
    }
}
