package com.example.finfit.finance.ui.wrappers

import com.example.finfit.finance.ui.screens.*
import com.example.finfit.finance.ui.utils.*

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import com.google.firebase.Timestamp
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.finfit.finance.model.*
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.ui.theme.PrimaryBlue
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun DashboardWithData(
    userEmail: String,
    firestoreRepository: FirestoreRepository,
    refreshTrigger: Int,
    onLogout: () -> Unit,
    onAction: (TransactionType?) -> Unit,
    onNavigate: (String) -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Sử dụng collectAsState để lắng nghe thời gian thực các thay đổi từ Firestore
    // Điều này giải quyết vấn đề Thông báo ngân hàng không cập nhật UI ngay lập tức
    val walletSource by if (user != null) {
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<AppUserWallet?>(null) }
    }

    val transactionsSource by if (user != null) {
        firestoreRepository.observeTransactions(user.uid).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf<List<FinanceTransaction>>(emptyList()) }
    }

    val goalsSource by if (user != null) {
        firestoreRepository.observeSavingsGoals(user.uid).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf<List<SavingsGoal>>(emptyList()) }
    }

    val budgetsSource by if (user != null) {
        firestoreRepository.observeBudgets(user.uid).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf<List<FinanceBudget>>(emptyList()) }
    }

    // Migration logic nếu user chưa có accounts sau bản cập nhật mới
    LaunchedEffect(walletSource) {
        val w = walletSource ?: return@LaunchedEffect
        if (w.accounts.isEmpty() && (w.savingsAmount > 0 || w.card1Name != "THẺ CHÍNH")) {
            val migratedAccounts = listOf(
                AppBankAccount(
                    id = UUID.randomUUID().toString(),
                    bankCode = "CASH",
                    name = w.card2Name,
                    amount = w.disposableAmount,
                    colorIndex = w.card2Color
                ),
                AppBankAccount(
                    id = UUID.randomUUID().toString(),
                    bankCode = "MB",
                    name = w.card1Name,
                    amount = w.savingsAmount,
                    colorIndex = w.card1Color
                )
            )
            firestoreRepository.saveUserWallet(w.copy(accounts = migratedAccounts))
        }
    }

    val isActuallyLoading = walletSource == null && user != null
    if (isActuallyLoading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        DashboardScreen(
            userEmail = userEmail,
            wallet = walletSource,
            transactions = transactionsSource,
            goals = goalsSource,
            budgets = budgetsSource,
            onSilentSave = { updated ->
                scope.launch { try { firestoreRepository.saveUserWallet(updated) } catch (e: Exception) { Log.e("SilentSave", e.message ?: "") } }
            },
            onDeleteTransaction = { txId ->
                if (user != null) {
                    scope.launch {
                        try {
                            // Tự động hoàn lại tiền vào ví khi xóa giao dịch
                            val txToDelete = transactionsSource.find { it.id == txId }
                            val currentWallet = walletSource
                            if (txToDelete != null && currentWallet != null) {
                                val accId = txToDelete.accountId
                                val updatedAccounts = currentWallet.accounts.map { acc ->
                                    when (txToDelete.type) {
                                        TransactionType.INCOME -> if (acc.id == accId) acc.copy(amount = acc.amount - txToDelete.amount) else acc
                                        TransactionType.EXPENSE -> if (acc.id == accId) acc.copy(amount = acc.amount + txToDelete.amount) else acc
                                        TransactionType.TRANSFER -> {
                                            when (acc.id) {
                                                txToDelete.accountId -> acc.copy(amount = acc.amount + txToDelete.amount)
                                                txToDelete.toAccountId -> acc.copy(amount = acc.amount - txToDelete.amount)
                                                else -> acc
                                            }
                                        }
                                    }
                                }
                                firestoreRepository.saveUserWallet(currentWallet.copy(accounts = updatedAccounts))
                            }
                            
                            firestoreRepository.deleteTransaction(user.uid, txId)
                            Toast.makeText(context, "Đã xóa giao dịch và cập nhật lại ví!", Toast.LENGTH_SHORT).show()
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
                            // Tự động điều chỉnh tiền trong ví khi sửa giao dịch
                            val oldTx = transactionsSource.find { it.id == updatedTx.id }
                            val currentWallet = walletSource
                            if (oldTx != null && currentWallet != null) {
                                // 1. Hoàn lại tiền cũ
                                var tempAccounts = currentWallet.accounts.map { acc ->
                                    when (oldTx.type) {
                                        TransactionType.INCOME -> if (acc.id == oldTx.accountId) acc.copy(amount = acc.amount - oldTx.amount) else acc
                                        TransactionType.EXPENSE -> if (acc.id == oldTx.accountId) acc.copy(amount = acc.amount + oldTx.amount) else acc
                                        TransactionType.TRANSFER -> {
                                            when (acc.id) {
                                                oldTx.accountId -> acc.copy(amount = acc.amount + oldTx.amount)
                                                oldTx.toAccountId -> acc.copy(amount = acc.amount - oldTx.amount)
                                                else -> acc
                                            }
                                        }
                                    }
                                }
                                
                                // 2. Áp dụng tiền mới
                                val finalAccounts = tempAccounts.map { acc ->
                                    when (updatedTx.type) {
                                        TransactionType.INCOME -> if (acc.id == updatedTx.accountId) acc.copy(amount = acc.amount + updatedTx.amount) else acc
                                        TransactionType.EXPENSE -> if (acc.id == updatedTx.accountId) acc.copy(amount = acc.amount - updatedTx.amount) else acc
                                        TransactionType.TRANSFER -> {
                                            when (acc.id) {
                                                updatedTx.accountId -> acc.copy(amount = acc.amount - updatedTx.amount)
                                                updatedTx.toAccountId -> acc.copy(amount = acc.amount + updatedTx.amount)
                                                else -> acc
                                            }
                                        }
                                    }
                                }
                                firestoreRepository.saveUserWallet(currentWallet.copy(accounts = finalAccounts))
                            }

                            firestoreRepository.updateTransaction(user.uid, updatedTx)
                            Toast.makeText(context, "Đã cập nhật giao dịch và ví!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi khi cập nhật: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onAction = onAction,
            onNavigate = onNavigate
        )
    }
}

@Composable
fun WalletManagementWithData(
    firestoreRepository: FirestoreRepository,
    onNavigate: (String) -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val walletSource by if (user != null) {
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<AppUserWallet?>(null) }
    }

    if (walletSource == null && user != null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        WalletManagementScreen(
            wallet = walletSource,
            onSaveWallet = { updated ->
                scope.launch {
                    try { firestoreRepository.saveUserWallet(updated); Toast.makeText(context, "Đã cập nhật!", Toast.LENGTH_SHORT).show() }
                    catch (e: Exception) { Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            },
            onNavigate = onNavigate
        )
    }
}

@Composable
fun AddTransactionWithData(
    firestoreRepository: FirestoreRepository,
    onTransactionSaved: () -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    initialTypeArg: String? = null
) {
    val initialType = try { initialTypeArg?.let { TransactionType.valueOf(it) } ?: TransactionType.EXPENSE } catch(e: Exception) { TransactionType.EXPENSE }
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
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
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
                            // onBack() // Bỏ: Vì onTransactionSaved đã gọi popBackStack() trong MainActivity rồi
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                            isSaving = false
                        }
                    }
                },
                onBack = onBack,
                onHome = onHome
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

@Composable
fun TransactionHistoryWithData(
    firestoreRepository: FirestoreRepository,
    onBack: () -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Sử dụng s trạng thái màn hình để hỗ trợ Sửa giao dịch từ Lịch sử
    var screenState by remember { mutableStateOf<DashboardScreenState>(DashboardScreenState.Home) }

    val transactions by if (user != null) {
        firestoreRepository.observeTransactions(user.uid).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf<List<FinanceTransaction>>(emptyList()) }
    }

    val wallet by if (user != null) {
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<AppUserWallet?>(null) }
    }

    when (val s = screenState) {
        is DashboardScreenState.Home -> {
            TransactionHistoryScreen(
                transactions = transactions,
                onEditTransaction = { tx -> screenState = DashboardScreenState.EditTransaction(tx) },
                onBack = onBack
            )
        }
        is DashboardScreenState.EditTransaction -> {
            EditTransactionScreen(
                transaction = s.transaction,
                onSave = { updatedTx ->
                    if (user != null) {
                        scope.launch {
                            try {
                                // Tự động điều chỉnh tiền trong ví khi sửa giao dịch (logic tương tự Dashboard)
                                val oldTx = transactions.find { it.id == updatedTx.id }
                                if (oldTx != null && wallet != null) {
                                    var tempAccounts = wallet!!.accounts.map { acc ->
                                        when (oldTx.type) {
                                            TransactionType.INCOME -> if (acc.id == oldTx.accountId) acc.copy(amount = acc.amount - oldTx.amount) else acc
                                            TransactionType.EXPENSE -> if (acc.id == oldTx.accountId) acc.copy(amount = acc.amount + oldTx.amount) else acc
                                            TransactionType.TRANSFER -> {
                                                when (acc.id) {
                                                    oldTx.accountId -> acc.copy(amount = acc.amount + oldTx.amount)
                                                    oldTx.toAccountId -> acc.copy(amount = acc.amount - oldTx.amount)
                                                    else -> acc
                                                }
                                            }
                                        }
                                    }
                                    val finalAccounts = tempAccounts.map { acc ->
                                        when (updatedTx.type) {
                                            TransactionType.INCOME -> if (acc.id == updatedTx.accountId) acc.copy(amount = acc.amount + updatedTx.amount) else acc
                                            TransactionType.EXPENSE -> if (acc.id == updatedTx.accountId) acc.copy(amount = acc.amount - updatedTx.amount) else acc
                                            TransactionType.TRANSFER -> {
                                                when (acc.id) {
                                                    updatedTx.accountId -> acc.copy(amount = acc.amount - updatedTx.amount)
                                                    updatedTx.toAccountId -> acc.copy(amount = acc.amount + updatedTx.amount)
                                                    else -> acc
                                                }
                                            }
                                        }
                                    }
                                    firestoreRepository.saveUserWallet(wallet!!.copy(accounts = finalAccounts))
                                }
                                firestoreRepository.updateTransaction(user.uid, updatedTx)
                                Toast.makeText(context, "Đã cập nhật!", Toast.LENGTH_SHORT).show()
                                screenState = DashboardScreenState.Home
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onDelete = { txId ->
                    if (user != null) {
                        scope.launch {
                            try {
                                val txToDelete = transactions.find { it.id == txId }
                                if (txToDelete != null && wallet != null) {
                                    val updatedAccounts = wallet!!.accounts.map { acc ->
                                        when (txToDelete.type) {
                                            TransactionType.INCOME -> if (acc.id == txToDelete.accountId) acc.copy(amount = acc.amount - txToDelete.amount) else acc
                                            TransactionType.EXPENSE -> if (acc.id == txToDelete.accountId) acc.copy(amount = acc.amount + txToDelete.amount) else acc
                                            TransactionType.TRANSFER -> {
                                                when (acc.id) {
                                                    txToDelete.accountId -> acc.copy(amount = acc.amount + txToDelete.amount)
                                                    txToDelete.toAccountId -> acc.copy(amount = acc.amount - txToDelete.amount)
                                                    else -> acc
                                                }
                                            }
                                        }
                                    }
                                    firestoreRepository.saveUserWallet(wallet!!.copy(accounts = updatedAccounts))
                                }
                                firestoreRepository.deleteTransaction(user.uid, txId)
                                Toast.makeText(context, "Đã xóa giao dịch!", Toast.LENGTH_SHORT).show()
                                screenState = DashboardScreenState.Home
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onBack = { screenState = DashboardScreenState.Home },
                onHome = { screenState = DashboardScreenState.Home }
            )
        }
    }
}

@Composable
fun SavingsGoalWithData(
    firestoreRepository: FirestoreRepository,
    onBack: () -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val goals by if (user != null) {
        firestoreRepository.observeSavingsGoals(user.uid).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf<List<SavingsGoal>>(emptyList()) }
    }
    
    // Ở mục tiêu tiết kiệm, nếu danh sách rỗng vẫn cho vào màn hình (không hiện xoay)
    SavingsGoalScreen(
        uid = user?.uid ?: "",
        goals = goals,
        onSaveGoal = { goal ->
            scope.launch {
                try {
                    if (user != null) {
                        firestoreRepository.saveSavingsGoal(user.uid, goal)
                        Toast.makeText(context, "Đã lưu mục tiêu!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        },
        onDeleteGoal = { goalId ->
            scope.launch {
                try {
                    if (user != null) {
                        firestoreRepository.deleteSavingsGoal(user.uid, goalId)
                        Toast.makeText(context, "Đã xóa mục tiêu!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        },
        onBack = onBack
    )
}

@Composable
fun DebtLoanWrapper(
    uid: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val items by repository.observeDebtLoans(uid).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    DebtLoanScreen(
        items = items,
        onSave = { dl -> scope.launch { repository.saveDebtLoan(uid, dl) } },
        onTogglePaid = { id, paid -> scope.launch { repository.toggleDebtLoanPaidStatus(uid, id, paid) } },
        onDelete = { id -> scope.launch { repository.deleteDebtLoan(uid, id) } },
        onBack = onBack
    )
}

@Composable
fun AnalyticsWrapper(
    uid: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val transactions by repository.observeTransactions(uid).collectAsState(initial = emptyList())
    
    AnalyticsScreen(
        transactions = transactions,
        onBack = onBack
    )
}

@Composable
fun BudgetWrapper(
    uid: String,
    onNavigateBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val budgets by repository.observeBudgets(uid).collectAsState(initial = emptyList())
    val transactions by repository.observeTransactions(uid).collectAsState(initial = emptyList())
    val wallet by repository.observeUserWallet(uid).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    
    BudgetScreen( // Explicitly passing all 7 parameters
        budgets = budgets,
        transactions = transactions,
        autoSaveSurplus = wallet?.autoSaveWeeklySurplus ?: false,
        onToggleAutoSave = { enabled ->
            wallet?.let { 
                scope.launch { 
                    repository.saveUserWallet(it.copy(autoSaveWeeklySurplus = enabled)) 
                }
            }
        },
        onSaveBudget = { budget ->
            scope.launch {
                repository.saveBudget(uid, budget)
            }
        },
        onDeleteBudget = { budgetId ->
            scope.launch {
                repository.deleteBudget(uid, budgetId)
            }
        },
        onBack = onNavigateBack
    )
}

@Composable
fun GeneralSavingsWithData(
    firestoreRepository: FirestoreRepository,
    onBack: () -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val walletSource by if (user != null) {
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<AppUserWallet?>(null) }
    }
    
    if (walletSource == null && user != null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        GeneralSavingsScreen(
            wallet = walletSource,
            onSaveWallet = { updated ->
                scope.launch {
                    try {
                        firestoreRepository.saveUserWallet(updated);
                   } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onBack = onBack
        )
    }
}

@Composable
fun HeldFundsWithData(
    firestoreRepository: FirestoreRepository,
    onBack: () -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val walletSource by if (user != null) {
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<AppUserWallet?>(null) }
    }
    
    if (walletSource == null && user != null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        HeldFundsManagementScreen(
            wallet = walletSource,
            onSaveWallet = { updated ->
                scope.launch {
                    try {
                        firestoreRepository.saveUserWallet(updated)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onBack = onBack
        )
    }
}
@Composable
fun InternalTransferWithDataFixed(
    firestoreRepository: FirestoreRepository,
    onBack: () -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val walletSource by if (user != null) {
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<AppUserWallet?>(null) }
    }
    
    if (walletSource == null && user != null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        InternalTransferScreen(
            wallet = walletSource!!,
            onNavigateBack = onBack,
            onConfirmTransfer = { fromId, toId, amount, note ->
                val currentWallet = walletSource!!
                val updatedAccounts = currentWallet.accounts.map { acc ->
                    when (acc.id) {
                        fromId -> acc.copy(amount = acc.amount - amount)
                        toId -> acc.copy(amount = acc.amount + amount)
                        else -> acc
                    }
                }
                
                scope.launch {
                    try {
                        val updatedWallet = currentWallet.copy(accounts = updatedAccounts)
                        firestoreRepository.saveUserWallet(updatedWallet)
                        
                        // Thêm bản ghi giao dịch cho lịch sử
                        val txId = UUID.randomUUID().toString()
                        val transferTx = FinanceTransaction(
                            id = txId,
                            amount = amount,
                            type = TransactionType.TRANSFER,
                            category = "Chuyển tiền nội bộ",
                            note = note,
                            timestamp = Timestamp.now(),
                            accountId = fromId,
                            toAccountId = toId,
                            paymentMethod = PaymentMethod.BANKING // Mặc định cho chuyển khoản nội bộ
                        )
                        firestoreRepository.addTransaction(user!!.uid, transferTx)

                        Toast.makeText(context, "Chuyển tiền thành công!", Toast.LENGTH_SHORT).show()
                        onBack()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}
