package com.example.finfit.finance.domain.repository

import com.example.finfit.finance.model.AppUserWallet
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun observeUserWallet(userId: String): Flow<AppUserWallet?>
    suspend fun getUserWallet(userId: String): AppUserWallet?
    suspend fun saveUserWallet(wallet: AppUserWallet)
}
