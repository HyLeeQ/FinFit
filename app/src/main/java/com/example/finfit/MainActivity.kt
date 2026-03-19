package com.example.finfit

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finfit.core.navigation.Routes
import com.example.finfit.data.model.BankAccount
import com.example.finfit.data.model.Transaction
import com.example.finfit.data.model.UserWallet
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.data.repository.FirestoreRepository
import com.example.finfit.ui.AddTransactionScreen
import com.example.finfit.ui.AuthScreen
import com.example.finfit.ui.DashboardScreen
import com.example.finfit.ui.MainScreen
import com.example.finfit.ui.theme.FinFitTheme
import com.example.finfit.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinFitTheme {
                val navController = rememberNavController()
                val currentUser = authRepository.getCurrentUser()
                val startDestination = if (currentUser == null) Routes.AUTH else Routes.MAIN

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.AUTH) {
                        AuthScreen(
                            authRepository = authRepository,
                            onLoginSuccess = {
                                navController.navigate(Routes.MAIN) {
                                    popUpTo(Routes.AUTH) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.MAIN) {
                        val user = authRepository.getCurrentUser()
                        if (user != null) {
                            var refreshTrigger by remember { mutableStateOf(0) }
                            MainScreen(
                                userEmail = user.email ?: "User",
                                firestoreRepository = firestoreRepository,
                                refreshTrigger = refreshTrigger,
                                onTransactionSaved = { refreshTrigger++ },
                                onLogout = {
                                    authRepository.signOut()
                                    navController.navigate(Routes.AUTH) {
                                        popUpTo(Routes.MAIN) { inclusive = true }
                                    }
                                }
                            )
                        } else {
                            navController.navigate(Routes.AUTH) { popUpTo(0) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardWithData(
    userEmail: String,
    firestoreRepository: FirestoreRepository,
    refreshTrigger: Int,
    onLogout: () -> Unit
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
            onLogout = onLogout
        )
    }
}

@Composable
fun AddTransactionWithData(
    firestoreRepository: FirestoreRepository,
    onTransactionSaved: () -> Unit,
    onBack: () -> Unit
) {
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
