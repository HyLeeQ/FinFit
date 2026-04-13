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
import com.example.finfit.finance.repository.*
import com.example.finfit.ui.theme.PrimaryBlue
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

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
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = FirestoreRepository.cachedWallet)
    } else {
        remember { mutableStateOf<AppUserWallet?>(FirestoreRepository.cachedWallet) }
    }

    val transactionsSource by if (user != null) {
        firestoreRepository.observeTransactions(user.uid).collectAsState(initial = FirestoreRepository.cachedTransactions)
    } else {
        remember { mutableStateOf<List<FinanceTransaction>>(FirestoreRepository.cachedTransactions) }
    }

    val goalsSource by if (user != null) {
        firestoreRepository.observeSavingsGoals(user.uid).collectAsState(initial = FirestoreRepository.cachedGoals)
    } else {
        remember { mutableStateOf<List<SavingsGoal>>(FirestoreRepository.cachedGoals) }
    }

    val budgetsSource by if (user != null) {
        firestoreRepository.observeBudgets(user.uid).collectAsState(initial = FirestoreRepository.cachedBudgets)
    } else {
        remember { mutableStateOf<List<FinanceBudget>>(FirestoreRepository.cachedBudgets) }
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
            schedule = if (user != null) firestoreRepository.observeWeeklySchedule(user.uid).collectAsState(initial = FirestoreRepository.cachedWeeklySchedule).value else emptyList(),
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
                                        TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> if (acc.id == accId) acc.copy(amount = acc.amount + txToDelete.amount) else acc
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
                                        TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> if (acc.id == oldTx.accountId) acc.copy(amount = acc.amount + oldTx.amount) else acc
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
                                        TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> if (acc.id == updatedTx.accountId) acc.copy(amount = acc.amount - updatedTx.amount) else acc
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
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = FirestoreRepository.cachedWallet)
    } else {
        remember { mutableStateOf<AppUserWallet?>(FirestoreRepository.cachedWallet) }
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
                    try { firestoreRepository.saveUserWallet(updated) }
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
    val transactions by firestoreRepository.observeTransactions(user?.uid ?: "").collectAsState(initial = FirestoreRepository.cachedTransactions)
    val budgets by firestoreRepository.observeBudgets(user?.uid ?: "").collectAsState(initial = FirestoreRepository.cachedBudgets)
    var isLoading by remember { mutableStateOf(wallet == null && user != null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(user?.uid) {
        if (user != null) {
            val fetchedWallet = firestoreRepository.getUserWallet(user.uid)
            wallet = fetchedWallet
            FirestoreRepository.cachedWallet = fetchedWallet
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
                budgets = budgets,
                transactions = transactions,
                initialType = initialType,
                onSave = { transaction, updatedWallet, imageUri ->
                    if (isSaving) return@AddTransactionScreen
                    wallet = updatedWallet
                    isSaving = true
                    scope.launch {
                        try {
                            // Upload ảnh lên Firebase Storage (nếu có)
                            val finalImageUrl: String? = if (imageUri != null && user != null) {
                                try {
                                    val storageRef = FirebaseStorage.getInstance()
                                        .reference
                                        .child("transaction_photos/${user.uid}/${transaction.id}.jpg")
                                    storageRef.putFile(imageUri).await()
                                    storageRef.downloadUrl.await().toString()
                                } catch (e: Exception) {
                                    android.util.Log.e("PhotoUpload", "Lỗi upload ảnh: ${e.message}")
                                    null
                                }
                            } else null

                            val txWithImage = if (finalImageUrl != null) {
                                transaction.copy(imageUrl = finalImageUrl)
                            } else transaction

                            firestoreRepository.saveUserWallet(updatedWallet)
                            if (user != null) firestoreRepository.addTransaction(user.uid, txWithImage)

                            // ── Tự động trừ generalSavings khi chi vượt hạn mức budget ───────
                            if (transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.GROUP_PREPAYMENT) {
                                val currentWallet = firestoreRepository.getUserWallet(user?.uid ?: "")
                                if (currentWallet != null && currentWallet.generalSavings > 0) {
                                    val cal = java.util.Calendar.getInstance()
                                    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    cal.set(java.util.Calendar.MINUTE, 0)
                                    cal.set(java.util.Calendar.SECOND, 0)
                                    val monthStart = cal.timeInMillis

                                    val matchBudget = budgets.find { it.category == transaction.category }
                                    if (matchBudget != null) {
                                        val spentThisMonth = (transactions + transaction).filter {
                                            it.category == transaction.category &&
                                            it.type == TransactionType.EXPENSE &&
                                            it.timestamp.toDate().time >= monthStart
                                        }.sumOf { it.amount }

                                        val excess = spentThisMonth - matchBudget.amount
                                        if (excess > 0) {
                                            val deduct = excess.coerceAtMost(currentWallet.generalSavings)
                                            firestoreRepository.saveUserWallet(
                                                currentWallet.copy(generalSavings = currentWallet.generalSavings - deduct)
                                            )
                                            Toast.makeText(
                                                context,
                                                "⚠️ Vượt hạn mức ${matchBudget.category}! Đã trừ ${formatCurrency(deduct)} từ quỹ dự phòng.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                            // ─────────────────────────────────────────────────────────────────

                            onTransactionSaved()
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
        firestoreRepository.observeTransactions(user.uid).collectAsState(initial = FirestoreRepository.cachedTransactions)
    } else {
        remember { mutableStateOf<List<FinanceTransaction>>(FirestoreRepository.cachedTransactions) }
    }

    val wallet by if (user != null) {
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = FirestoreRepository.cachedWallet)
    } else {
        remember { mutableStateOf<AppUserWallet?>(FirestoreRepository.cachedWallet) }
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
                                            TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> if (acc.id == oldTx.accountId) acc.copy(amount = acc.amount + oldTx.amount) else acc
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
                                            TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> if (acc.id == updatedTx.accountId) acc.copy(amount = acc.amount - updatedTx.amount) else acc
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
                                            TransactionType.EXPENSE, TransactionType.GROUP_PREPAYMENT -> if (acc.id == txToDelete.accountId) acc.copy(amount = acc.amount + txToDelete.amount) else acc
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
        firestoreRepository.observeSavingsGoals(user.uid).collectAsState(initial = FirestoreRepository.cachedGoals)
    } else {
        remember { mutableStateOf<List<SavingsGoal>>(FirestoreRepository.cachedGoals) }
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
    val items by repository.observeDebtLoans(uid).collectAsState(initial = FirestoreRepository.cachedDebtLoans)
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
    val transactions by repository.observeTransactions(uid).collectAsState(initial = FirestoreRepository.cachedTransactions)
    
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
    val budgets by repository.observeBudgets(uid).collectAsState(initial = FirestoreRepository.cachedBudgets)
    val transactions by repository.observeTransactions(uid).collectAsState(initial = FirestoreRepository.cachedTransactions)
    val wallet by repository.observeUserWallet(uid).collectAsState(initial = FirestoreRepository.cachedWallet)
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
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = FirestoreRepository.cachedWallet)
    } else {
        remember { mutableStateOf<AppUserWallet?>(FirestoreRepository.cachedWallet) }
    }
    // Quan sát mục tiêu tiết kiệm cá nhân để hiển thị và trích tiền
    val goals by if (user != null) {
        firestoreRepository.observeSavingsGoals(user.uid).collectAsState(initial = FirestoreRepository.cachedGoals)
    } else {
        remember { mutableStateOf<List<SavingsGoal>>(FirestoreRepository.cachedGoals) }
    }

    if (walletSource == null && user != null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
    } else {
        GeneralSavingsScreen(
            wallet = walletSource,
            goals = goals,
            onSaveWallet = { updated ->
                scope.launch {
                    try { firestoreRepository.saveUserWallet(updated) }
                    catch (e: Exception) { Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            },
            onSaveGoal = { goal ->
                scope.launch {
                    try {
                        if (user != null) firestoreRepository.saveSavingsGoal(user.uid, goal)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi cập nhật mục tiêu: ${e.message}", Toast.LENGTH_LONG).show()
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
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = FirestoreRepository.cachedWallet)
    } else {
        remember { mutableStateOf<AppUserWallet?>(FirestoreRepository.cachedWallet) }
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
        firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = FirestoreRepository.cachedWallet)
    } else {
        remember { mutableStateOf<AppUserWallet?>(FirestoreRepository.cachedWallet) }
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
@Composable
fun WeeklyScheduleWrapper(
    uid: String,
    firestoreRepository: FirestoreRepository,
    onBack: () -> Unit
) {
    val items by firestoreRepository.observeWeeklySchedule(uid).collectAsState(initial = FirestoreRepository.cachedWeeklySchedule)
    val scope = rememberCoroutineScope()
    
    WeeklyScheduleScreen(
        items = items,
        onSave = { item -> scope.launch { firestoreRepository.saveWeeklyScheduleItem(uid, item) } },
        onDelete = { id -> scope.launch { firestoreRepository.deleteWeeklyScheduleItem(uid, id) } },
        onBack = onBack
    )
}

@Composable
fun PhotoDiaryWithData(
    firestoreRepository: FirestoreRepository,
    onBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    val transactions by if (user != null) {
        firestoreRepository.observeTransactions(user.uid, limit = 200).collectAsState(initial = FirestoreRepository.cachedTransactions)
    } else {
        remember { mutableStateOf<List<FinanceTransaction>>(FirestoreRepository.cachedTransactions) }
    }

    PhotoDiaryScreen(
        transactions = transactions,
        onBack = onBack,
        onAddClick = onNavigateToAddTransaction
    )
}
