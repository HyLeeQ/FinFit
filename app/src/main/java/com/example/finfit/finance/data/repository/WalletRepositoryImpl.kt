package com.example.finfit.finance.data.repository

import com.example.finfit.finance.domain.repository.WalletRepository
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.repository.FirestoreRepository
import kotlinx.coroutines.flow.Flow

class WalletRepositoryImpl(
    private val firestoreRepository: FirestoreRepository = FirestoreRepository()
) : WalletRepository {

    override fun observeUserWallet(userId: String): Flow<AppUserWallet?> {
        return firestoreRepository.observeUserWallet(userId)
    }

    override suspend fun getUserWallet(userId: String): AppUserWallet? {
        return firestoreRepository.getUserWallet(userId)
    }

    override suspend fun saveUserWallet(wallet: AppUserWallet) {
        firestoreRepository.saveUserWallet(wallet)
    }
}
