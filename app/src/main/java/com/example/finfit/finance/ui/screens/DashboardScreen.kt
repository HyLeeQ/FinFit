package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.finfit.core.navigation.Routes
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.*
import java.text.*
import java.util.*

// ─── Data Helper ─────────────────────────────────────────────
data class CalculatedFunds(
    val personal: Double,
    val goal: Double,
    val general: Double,
    val held: Double,
    val total: Double,
    val spendable: Double
)

// Categories (EXPENSE_CATEGORIES, INCOME_CATEGORIES, TRANSFER_CATEGORIES, TxCategory) 
// are now centrally defined in FinanceCategories.kt in the same package.

// ─── Entry point ─────────────────────────────────────────────
@Composable
fun DashboardScreen(
    userEmail: String,
    wallet: AppUserWallet?,
    transactions: List<FinanceTransaction>,
    goals: List<SavingsGoal>,
    budgets: List<FinanceBudget> = emptyList(),
    onSilentSave: (AppUserWallet) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onUpdateTransaction: (FinanceTransaction) -> Unit,
    onAction: (TransactionType?) -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    var screen by remember { mutableStateOf<DashboardScreenState>(DashboardScreenState.Home) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        when (val s = screen) {
            is DashboardScreenState.Home -> HomeContent(
                userEmail = userEmail,
                wallet = wallet,
                transactions = transactions,
                goals = goals,
                budgets = budgets,
                onSilentSave = onSilentSave,
                onAction = onAction,
                onEditTransaction = { tx -> screen = DashboardScreenState.EditTransaction(tx) },
                onNavigate = onNavigate,
                onSavingsAction = { /* handled in HomeContent */ }
            )
            is DashboardScreenState.EditTransaction -> EditTransactionScreen(
                transaction = s.transaction,
                onSave = { updated -> onUpdateTransaction(updated); screen = DashboardScreenState.Home },
                onDelete = { id -> onDeleteTransaction(id); screen = DashboardScreenState.Home },
                onBack = { screen = DashboardScreenState.Home },
                onHome = { screen = DashboardScreenState.Home }
            )
        }
    }
}

// Renamed to avoid conflicts with other 'Screen' names if any
sealed class DashboardScreenState {
    object Home : DashboardScreenState()
    data class EditTransaction(val transaction: FinanceTransaction) : DashboardScreenState()
}

@Composable
fun HomeContent(
    userEmail: String,
    wallet: AppUserWallet?,
    transactions: List<FinanceTransaction>,
    goals: List<SavingsGoal>,
    budgets: List<FinanceBudget>,
    onSilentSave: (AppUserWallet) -> Unit,
    onAction: (TransactionType?) -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onNavigate: (String) -> Unit,
    onSavingsAction: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDashboardHidden = wallet?.isTotalBalanceHidden ?: true

    // Animation control
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { 
            HeaderSectionWithAnim(userEmail, visible) 
        }
        item { Spacer(Modifier.height(20.dp)) }

        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600)) + expandVertically(animationSpec = tween(600))
            ) {
                val funds = remember(wallet, goals) {
                    val personal = wallet?.totalBalance ?: 0.0
                    val goal = goals.sumOf { it.currentAmount }
                    val general = wallet?.generalSavings ?: 0.0
                    val held = wallet?.totalHeldFunds ?: 0.0
                    val total = personal + held
                    val spend = (personal - goal - general).coerceAtLeast(0.0)
                    CalculatedFunds(personal, goal, general, held, total, spend)
                }

                FundDistributionSection(
                    totalManaged = funds.total,
                    personalMoney = funds.personal,
                    spendable = funds.spendable,
                    goalCommitted = funds.goal,
                    generalSaved = funds.general,
                    heldFunds = funds.held,
                    onAdjustGeneral = { onNavigate(Routes.GENERAL_SAVINGS) },
                    isHiddenGlobal = isDashboardHidden,
                    onToggleVisible = {
                        wallet?.let { 
                            onSilentSave(it.copy(isTotalBalanceHidden = !isDashboardHidden)) 
                            Toast.makeText(context, if (isDashboardHidden) "Hiện số dư tổng" else "Ẩn số dư tổng", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        item {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(animationSpec = tween(700), initialOffsetX = { it/2 }) + fadeIn(animationSpec = tween(700))
            ) {
                val accounts = wallet?.accounts ?: emptyList()
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            isHiddenGlobal = isDashboardHidden,
                            onToggle = {
                                if (wallet != null) {
                                    val updated = wallet.copy(
                                        accounts = wallet.accounts.map {
                                            if (it.id == account.id) it.copy(isHidden = !it.isHidden) else it
                                        }
                                    )
                                    onSilentSave(updated)
                                }
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item { 
            QuickActionsSection(
                onAction = onAction,
                onSavingsAction = { onNavigate(Routes.GENERAL_SAVINGS) },
                onHeldFundsAction = { onNavigate(Routes.HELD_FUNDS) },
                onTransferAction = { onNavigate(Routes.TRANSFER) },
                onNavigate = onNavigate
            )
        }
        item { Spacer(Modifier.height(24.dp)) }

        // Mới: Tiến độ kế hoạch (Budget)
        item {
            PlanProgressSection(transactions, budgets, onNavigate)
        }
        item { Spacer(Modifier.height(32.dp)) }

        item { SavingsGoalsSection(goals, onNavigate) }
        item { Spacer(Modifier.height(32.dp)) }
        
        item { RecentTransactionsSection(transactions, onEditTransaction, onNavigate) }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
fun PlanProgressSection(transactions: List<FinanceTransaction>, budgets: List<FinanceBudget>, onNavigate: (String) -> Unit) {
    val now = remember { java.util.Calendar.getInstance() }
    val currentMonthExpenditure = remember(transactions) {
        transactions.filter { tx ->
            val txCal = java.util.Calendar.getInstance().apply { time = tx.timestamp.toDate() }
            tx.type == TransactionType.EXPENSE && 
            txCal.get(java.util.Calendar.MONTH) == now.get(java.util.Calendar.MONTH) &&
            txCal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)
        }.sumOf { it.amount }
    }
    
    val currentTotalBudget = remember(budgets) {
        budgets.filter { it.period == BudgetPeriod.MONTHLY && it.category == "Tất cả" }.sumOf { it.amount }
    }

    if (currentTotalBudget > 0) {
        val progress = (currentMonthExpenditure / currentTotalBudget).coerceIn(0.0, 1.0).toFloat()
        val remaining = (currentTotalBudget - currentMonthExpenditure).coerceAtLeast(0.0)
        
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tiến độ kế hoạch", fontWeight = FontWeight.Black, fontSize = 20.sp)
                TextButton(onClick = { onNavigate(Routes.BUDGET) }) {
                    Text("Chi tiết", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .clickable { onNavigate(Routes.BUDGET) }
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hạn mức tháng này", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            Text(formatCurrency(currentTotalBudget), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (progress > 0.9f) Color.Red.copy(alpha = 0.2f) else Color.Green.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (progress > 0.9f) "Sắp vượt hạn mức" else "Đang kiểm soát tốt",
                                color = if (progress > 0.9f) Color(0xFFFF4D4D) else Color(0xFF4ADE80),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = if (progress > 0.9f) Color(0xFFFF4D4D) else Color(0xFF3B82F6),
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Đã chi: ${formatCurrency(currentMonthExpenditure)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            "Còn lại: ${formatCurrency(remaining)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsGoalsSection(goals: List<SavingsGoal>, onNavigate: (String) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mục tiêu tiết kiệm", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = { onNavigate(Routes.SAVINGS_GOALS) }) {
                Text("Xem tất cả", color = PrimaryBlue, fontSize = 13.sp)
            }
        }
        if (goals.isEmpty()) {
            Text("Chưa có mục tiêu nào", color = Color.Gray, fontSize = 14.sp)
        } else {
            goals.take(3).forEach { goal ->
                val progress = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onNavigate(Routes.SAVINGS_GOALS) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(goal.iconEmoji, fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(goal.goalName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = Color(goal.colorHex))
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = Color(goal.colorHex),
                            trackColor = Color(goal.colorHex).copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<FinanceTransaction>,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onNavigate: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Giao dịch gần đây", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = { onNavigate(Routes.TRANSACTION_HISTORY) }) {
                Text("Xem tất cả", color = PrimaryBlue, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (transactions.isEmpty()) {
            Text("Chưa có giao dịch nào", color = Color.Gray, fontStyle = FontStyle.Italic)
        } else {
            transactions.take(5).forEach { transaction ->
                TransactionListItem(transaction) { onEditTransaction(transaction) }
                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun EditTransactionScreen(transaction: FinanceTransaction, onSave: (FinanceTransaction) -> Unit, onDelete: (String) -> Unit, onBack: () -> Unit, onHome: () -> Unit) {
    var amountText by remember { mutableStateOf(transaction.amount.toLong().toString()) }
    var noteText by remember { mutableStateOf(transaction.note) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa giao dịch?") },
            text = { Text("Bạn có chắc muốn xóa vĩnh viễn giao dịch này?") },
            confirmButton = { TextButton(onClick = { onDelete(transaction.id) }) { Text("Xóa", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Chi tiết giao dịch", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Số tiền", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("đ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = amountText, onValueChange = { amountText = it },
                        textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Danh mục", fontWeight = FontWeight.Bold)
        val allCats = (EXPENSE_CATEGORIES + INCOME_CATEGORIES + TRANSFER_CATEGORIES).distinctBy { it.label }
        allCats.chunked(5).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val isSelected = selectedCategory == cat.label
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) cat.color.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { selectedCategory = cat.label }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(cat.color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(20.dp))
                        }
                        Text(cat.label, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onSave(transaction.copy(amount = amountText.toDoubleOrNull() ?: 0.0, note = noteText, category = selectedCategory)) },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Lưu thay đổi") }
        TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Xóa giao dịch", color = Color.Red) }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return (EXPENSE_CATEGORIES + INCOME_CATEGORIES + TRANSFER_CATEGORIES).find { it.label == category }?.icon ?: Icons.Default.Receipt
}

@Composable
fun AccountCard(account: AppBankAccount, isHiddenGlobal: Boolean, onToggle: () -> Unit) {
    val bankInfo = remember(account.bankCode) { SUPPORTED_BANKS.find { it.code == account.bankCode } ?: SUPPORTED_BANKS.last() }
    val gradientColors = remember(account.colorIndex, bankInfo.primaryColorHex) { cardGradient(account.colorIndex, bankInfo.primaryColorHex) }

    Box(modifier = Modifier.width(300.dp).height(170.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(gradientColors)).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bankInfo.emoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(bankInfo.displayName.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onToggle) { Icon(if (account.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.White.copy(alpha = 0.8f)) }
        }
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text("Số dư khả dụng", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            AnimatedAmountText(amount = account.amount, isHidden = isHiddenGlobal && account.isHidden, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Text(account.name.uppercase(), color = Color.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomStart))
    }
}

@Composable
fun HeaderSectionWithAnim(userEmail: String, isVisible: Boolean) {
    AnimatedVisibility(visible = isVisible, enter = slideInVertically() + fadeIn()) { HeaderSection(userEmail) }
}

@Composable
fun HeaderSection(userEmail: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Xin chào 👋", fontSize = 12.sp)
                Text(userEmail.substringBefore("@"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
        IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null) }
    }
}

@Composable
fun QuickActionsSection(onAction: (TransactionType?) -> Unit, onSavingsAction: () -> Unit, onHeldFundsAction: () -> Unit, onTransferAction: () -> Unit, onNavigate: (String) -> Unit) {
    val actions = listOf(
        Triple("Giao dịch", Icons.Default.Add, PrimaryBlue),
        Triple("Tiết kiệm", Icons.Default.Savings, Color(0xFF10B981)),
        Triple("Ví nhóm", Icons.Default.Groups, Color(0xFFF59E0B)),
        Triple("Chuyển tiền", Icons.Default.SwapHoriz, Color(0xFF6366F1)),
        Triple("Thống kê", Icons.Default.BarChart, Color(0xFFEC4899)),
        Triple("Kế hoạch", Icons.Default.EventNote, Color(0xFF8B5CF6)),
        Triple("Nợ/Vay", Icons.Default.AccountBalance, Color(0xFF9333EA))
    )
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        actions.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, icon, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable {
                        when(label) {
                            "Giao dịch" -> onAction(null)
                            "Tiết kiệm" -> onSavingsAction()
                            "Ví nhóm" -> onHeldFundsAction()
                            "Chuyển tiền" -> onTransferAction()
                            "Thống kê" -> onNavigate(Routes.ANALYTICS)
                            "Kế hoạch" -> onNavigate(Routes.BUDGET)
                            "Nợ/Vay" -> onNavigate(Routes.DEBT_LOAN)
                        }
                    }) {
                        Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(icon, label, tint = color) }
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun TransactionListItem(transaction: FinanceTransaction, onClick: () -> Unit) {
    val color = if (transaction.type == TransactionType.INCOME) Color(0xFF10C67F) else if (transaction.type == TransactionType.EXPENSE) Color(0xFFEF4444) else Color(0xFF6366F1)
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(getCategoryIcon(transaction.category), null) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.note.ifBlank { transaction.category }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(SimpleDateFormat("HH:mm • dd/MM", Locale.getDefault()).format(transaction.timestamp.toDate()), fontSize = 12.sp)
        }
        Text("${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${formatCurrency(transaction.amount)}", color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FundDistributionSection(totalManaged: Double, personalMoney: Double, spendable: Double, goalCommitted: Double, generalSaved: Double, heldFunds: Double, onAdjustGeneral: () -> Unit, isHiddenGlobal: Boolean, onToggleVisible: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TÀI SẢN CÁ NHÂN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    AnimatedAmountText(amount = personalMoney, isHidden = isHiddenGlobal, color = MaterialTheme.colorScheme.onBackground, fontSize = 32.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onToggleVisible) { Icon(if (isHiddenGlobal) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                val base = totalManaged.coerceAtLeast(1.0)
                if (spendable > 0) Box(Modifier.fillMaxHeight().weight((spendable/base).toFloat()).background(PrimaryBlue))
                if (goalCommitted > 0) Box(Modifier.fillMaxHeight().weight((goalCommitted/base).toFloat()).background(Color(0xFFF59E0B)))
                if (generalSaved > 0) Box(Modifier.fillMaxHeight().weight((generalSaved/base).toFloat()).background(AccentGreen))
                if (heldFunds > 0) Box(Modifier.fillMaxHeight().weight((heldFunds/base).toFloat()).background(Color(0xFF8B5CF6)))
            }
            Spacer(Modifier.height(20.dp))
            FundItem("Sử dụng thoải mái", spendable, PrimaryBlue, Icons.Default.AccountBalanceWallet, isHiddenGlobal)
            FundItem("Mục tiêu tiết kiệm", goalCommitted, Color(0xFFF59E0B), Icons.Default.TrendingUp, isHiddenGlobal)
            FundItem("Tiết kiệm chung", generalSaved, AccentGreen, Icons.Default.Lock, isHiddenGlobal)
            FundItem("Ví nhóm", heldFunds, Color(0xFF8B5CF6), Icons.Default.Groups, isHiddenGlobal)
        }
    }
}

@Composable
fun FundItem(label: String, amount: Double, color: Color, icon: ImageVector, isHidden: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp)
        }
        AnimatedAmountText(amount, isHidden, MaterialTheme.colorScheme.onBackground, 14.sp, FontWeight.Bold, true)
    }
}

// AnimatedAmountText is now imported from com.example.finfit.finance.ui.utils.FinanceUtils

