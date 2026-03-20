package com.example.finfit.finance.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.example.finfit.finance.model.*
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.ui.theme.PrimaryBlue
import kotlinx.coroutines.*
import java.util.UUID

@Composable
fun DashboardWithData(
    userEmail: String,
    firestoreRepository: FirestoreRepository,
    refreshTrigger: Int,
    onLogout: () -> Unit,
    onAction: (TransactionType?) -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    var wallet by remember { mutableStateOf<AppUserWallet?>(null) }
    var transactions by remember { mutableStateOf<List<FinanceTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(user?.uid, refreshTrigger) {
        if (user != null) {
            // Chỉ hiện loading xoay ở lần đầu tiên vào màn hình
            if (wallet == null) isLoading = true
            
            try {
                // Chạy song song để tăng tốc độ (giảm "đơ" khi chuyển trang)
                val walletDeferred = async { firestoreRepository.getUserWallet(user.uid) }
                val txDeferred = async { firestoreRepository.getTransactions(user.uid) }
                
                val walletData = walletDeferred.await()
                transactions = txDeferred.await()
                
                wallet = if (walletData != null) {
                    if (walletData.accounts.isEmpty() && (walletData.savingsAmount > 0 || walletData.card1Name != "THẺ CHÍNH")) {
                        val migratedAccounts = buildList {
                            if (walletData.savingsAmount > 0 || walletData.card1Name != "THẺ CHÍNH") {
                                add(AppBankAccount(UUID.randomUUID().toString(), "MB", walletData.card1Name, walletData.savingsAmount, walletData.card1Color, walletData.isSavingsHidden))
                            }
                            if (walletData.disposableAmount > 0 || walletData.card2Name != "TIỀN MẶT") {
                                add(AppBankAccount(UUID.randomUUID().toString(), "CASH", walletData.card2Name, walletData.disposableAmount, walletData.card2Color, walletData.isDisposableHidden))
                            }
                        }
                        val migrated = walletData.copy(accounts = migratedAccounts)
                        firestoreRepository.saveUserWallet(migrated)
                        migrated
                    } else walletData
                } else {
                    val newWallet = AppUserWallet(uid = user.uid)
                    firestoreRepository.saveUserWallet(newWallet)
                    newWallet
                }
            } catch (e: Exception) {
                Log.e("FinanceWrappers", "Error fetching data: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        DashboardScreen(
            userEmail = userEmail,
            wallet = wallet,
            transactions = transactions,
            onSaveWallet = { updated ->
                wallet = updated
                scope.launch {
                    try { firestoreRepository.saveUserWallet(updated); Toast.makeText(context, "Đã cập nhật!", Toast.LENGTH_SHORT).show() }
                    catch (e: Exception) { Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            },
            onSilentSave = { updated ->
                wallet = updated
                scope.launch { try { firestoreRepository.saveUserWallet(updated) } catch (e: Exception) { Log.e("SilentSave", e.message ?: "") } }
            },
            onDeleteTransaction = { txId ->
                if (user != null) {
                    scope.launch {
                        try {
                            firestoreRepository.deleteTransaction(user.uid, txId)
                            Toast.makeText(context, "Đã xóa giao dịch!", Toast.LENGTH_SHORT).show()
                            // Kích hoạt load lại dữ liệu
                            val txList = firestoreRepository.getTransactions(user.uid)
                            transactions = txList
                            val data = firestoreRepository.getUserWallet(user.uid)
                            if (data != null) wallet = data
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi khi xóa: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onUpdateTransaction = { updatedTx ->
                if (user != null) {
                    scope.launch {
                        try {
                            firestoreRepository.updateTransaction(user.uid, updatedTx)
                            Toast.makeText(context, "Đã cập nhật giao dịch!", Toast.LENGTH_SHORT).show()
                            // Kích hoạt load lại dữ liệu
                            val txList = firestoreRepository.getTransactions(user.uid)
                            transactions = txList
                            val data = firestoreRepository.getUserWallet(user.uid)
                            if (data != null) wallet = data
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi khi cập nhật: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onLogout = onLogout,
            onAction = onAction
        )
    }
}

@Composable
fun AddTransactionWithData(
    firestoreRepository: FirestoreRepository,
    onTransactionSaved: () -> Unit,
    onBack: () -> Unit,
    initialTypeArg: String? = null
) {
    val initialType = try {
        if (initialTypeArg != null) TransactionType.valueOf(initialTypeArg) else TransactionType.EXPENSE
    } catch (e: Exception) {
        TransactionType.EXPENSE
    }
    val user = AuthRepository().getCurrentUser()
    var wallet by remember { mutableStateOf<AppUserWallet?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(user?.uid) {
        if (user != null) {
            wallet = firestoreRepository.getUserWallet(user.uid)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            AddTransactionScreen(
                wallet = wallet,
                initialType = initialType,
                onSave = { transaction, updatedWallet ->
                    if (isSaving) return@AddTransactionScreen
                    wallet = updatedWallet
                    isSaving = true
                    scope.launch {
                        try {
                            firestoreRepository.saveUserWallet(updatedWallet)
                            if (user != null) firestoreRepository.addTransaction(user.uid, transaction)
                            Toast.makeText(context, "Đã lưu giao dịch!", Toast.LENGTH_SHORT).show()
                            onTransactionSaved()
                            onBack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                            isSaving = false
                        }
                    }
                },
                onBack = onBack
            )
            
            // Lớp phủ ngăn người dùng tương tác trong khi lưu
            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PrimaryBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Đang lưu...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
