package com.example.finfit.finance.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.finfit.finance.model.BankAccount
import com.example.finfit.finance.model.Transaction
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.model.UserWallet
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch
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
    var wallet by remember { mutableStateOf<UserWallet?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(user?.uid, refreshTrigger) {
        if (user != null) {
            isLoading = (wallet == null) // Chỉ hiện loading to ở lần đầu
            val data = firestoreRepository.getUserWallet(user.uid)
            val txList = firestoreRepository.getTransactions(user.uid)
            transactions = txList
            
            wallet = if (data != null) {
                if (data.accounts.isEmpty() && (data.savingsAmount > 0 || data.disposableAmount > 0)) {
                    val migratedAccounts = buildList {
                        if (data.savingsAmount > 0 || data.card1Name != "THẺ CHÍNH") {
                            add(BankAccount(UUID.randomUUID().toString(), "MB", data.card1Name, data.savingsAmount, data.card1Color, data.isSavingsHidden))
                        }
                        if (data.disposableAmount > 0 || data.card2Name != "TIỀN MẶT") {
                            add(BankAccount(UUID.randomUUID().toString(), "CASH", data.card2Name, data.disposableAmount, data.card2Color, data.isDisposableHidden))
                        }
                    }
                    val migrated = data.copy(accounts = migratedAccounts)
                    firestoreRepository.saveUserWallet(migrated)
                    migrated
                } else data
            } else {
                val newWallet = UserWallet(uid = user.uid)
                firestoreRepository.saveUserWallet(newWallet)
                newWallet
            }
            isLoading = false
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
    var wallet by remember { mutableStateOf<UserWallet?>(null) }
    var isLoading by remember { mutableStateOf(true) }
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
        AddTransactionScreen(
            wallet = wallet,
            initialType = initialType,
            onSave = { transaction, updatedWallet ->
                wallet = updatedWallet
                scope.launch {
                    try {
                        firestoreRepository.saveUserWallet(updatedWallet)
                        if (user != null) firestoreRepository.addTransaction(user.uid, transaction)
                        Toast.makeText(context, "Đã lưu giao dịch!", Toast.LENGTH_SHORT).show()
                        onTransactionSaved()
                        onBack()
                    } catch (e: Exception) { Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            },
            onBack = onBack
        )
    }
}
