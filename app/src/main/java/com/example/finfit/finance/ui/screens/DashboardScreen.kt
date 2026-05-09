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
import com.example.finfit.finance.ui.utils.formatAmountInput

// ─── Data Helper ─────────────────────────────────────────────
data class CalculatedFunds(
    val personal: Double,
    val goal: Double,
    val general: Double,
    val held: Double,
    val total: Double,
    val spendable: Double
)

private val transactionDateFormat = java.text.SimpleDateFormat("HH:mm • dd/MM", java.util.Locale.getDefault())

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
    onNavigate: (String) -> Unit = {},
    schedule: List<SpendingScheduleItem> = emptyList()
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
                onSavingsAction = { /* handled in HomeContent */ },
                schedule = schedule
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
    onSavingsAction: (() -> Unit)? = null,
    schedule: List<SpendingScheduleItem> = emptyList()
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

        // --- Mới: Lịch trình chi tiêu hôm nay ---
        val today = ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7) + 1
        val todayPlannedItems = schedule.filter { it.dayOfWeek == today }
        
        if (todayPlannedItems.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kế hoạch hôm nay", fontSize = 16.sp, fontWeight = FontWeight.Black)
                        TextButton(onClick = { onNavigate(Routes.FINANCE_PLAN) }) {
                            Text("Xem lịch trình", fontSize = 12.sp, color = PrimaryBlue)
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(todayPlannedItems) { plan ->
                            val cat = EXPENSE_CATEGORIES.find { it.label == plan.category } ?: EXPENSE_CATEGORIES.last()
                            Card(
                                modifier = Modifier.width(160.dp).clickable { onNavigate("${Routes.ADD}?type=${TransactionType.EXPENSE.name}") },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = cat.color.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, cat.color.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(cat.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cat.color)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(formatCurrency(plan.amount), fontSize = 15.sp, fontWeight = FontWeight.Black)
                                    if (plan.note.isNotBlank()) {
                                        Text(plan.note, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

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
    val currentMonth = now.get(java.util.Calendar.MONTH)
    val currentYear = now.get(java.util.Calendar.YEAR)
    
    val currentMonthExpenditure = remember(transactions) {
        val tempCal = java.util.Calendar.getInstance()
        transactions.filter { tx ->
            if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT) {
                tempCal.time = tx.timestamp.toDate()
                tempCal.get(java.util.Calendar.MONTH) == currentMonth && tempCal.get(java.util.Calendar.YEAR) == currentYear
            } else false
        }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
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
            Text("Mục tiêu tiết kiệm", fontWeight = FontWeight.Black, fontSize = 18.sp)
            TextButton(onClick = { onNavigate(Routes.SAVINGS_GOALS) }) {
                Text("Xem tất cả →", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onNavigate(Routes.SAVINGS_GOALS) }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Chưa có mục tiêu nào", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 14.sp)
                    Text("Nhấn để tạo mục tiêu đầu tiên", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(goals) { goal ->
                    val progress = (goal.currentAmount / goal.targetAmount.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
                    val goalColor = Color(goal.colorHex)
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        goalColor.copy(alpha = 0.15f),
                                        goalColor.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .border(1.dp, goalColor.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .clickable { onNavigate(Routes.SAVINGS_GOALS) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(goal.iconEmoji, fontSize = 22.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = goalColor
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                goal.goalName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(10.dp))
                            // Gradient progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(goalColor.copy(alpha = 0.12f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(goalColor, goalColor.copy(alpha = 0.6f))
                                            )
                                        )
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                formatCurrency(goal.currentAmount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "/ ${formatCurrency(goal.targetAmount)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                            )
                        }
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
    // Lưu raw digits, hiển thị formatted
    var amountRaw by remember { mutableStateOf(transaction.amount.toLong().toString()) }
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
                    val displayAmt = formatAmountInput(amountRaw)
                    BasicTextField(
                        value = displayAmt,
                        onValueChange = { amountRaw = it.filter { c -> c.isDigit() } },
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
        Button(onClick = { onSave(transaction.copy(amount = amountRaw.toDoubleOrNull() ?: 0.0, note = noteText, category = selectedCategory)) },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Lưu thay đổi") }
        TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Xóa giao dịch", color = Color.Red) }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return (EXPENSE_CATEGORIES + INCOME_CATEGORIES + TRANSFER_CATEGORIES).find { it.label == category }?.icon ?: Icons.Default.Receipt
}

@Composable
fun AccountCard(account: AppBankAccount, onToggle: () -> Unit) {
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
            AnimatedAmountText(amount = account.amount, isHidden = account.isHidden, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
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
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11  -> "☀️ Chào buổi sáng"
        in 12..13 -> "🌞 Buổi trưa vui vẻ"
        in 14..17 -> "☀️ Buổi chiều"
        in 18..21 -> "🌇 Buổi tối"
        else      -> "🌙 Khuya rồi"
    }
    val displayName = userEmail.substringBefore("@").replaceFirstChar { it.uppercaseChar() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Gradient avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    displayName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(greeting, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Text(displayName, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
        // Notification bell with subtle badge
        Box {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.Notifications,
                    null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onAction: (TransactionType?) -> Unit,
    onSavingsAction: () -> Unit,
    onHeldFundsAction: () -> Unit,
    onTransferAction: () -> Unit,
    onNavigate: (String) -> Unit
) {
    data class Action(val label: String, val icon: ImageVector, val gradient: List<Color>)
    val actions = listOf(
        Action("➕ Giao dịch", Icons.Default.Add,             listOf(Color(0xFF3B82F6), Color(0xFF6366F1))),
        Action("🎯 Tiết kiệm", Icons.Default.Savings,         listOf(Color(0xFF10B981), Color(0xFF059669))),
        Action("👥 Ví nhóm",  Icons.Default.Groups,          listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
        Action("🔄 Chuyển tiền", Icons.Default.SwapHoriz,      listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
        Action("📊 Thống kê", Icons.Default.BarChart,         listOf(Color(0xFFEC4899), Color(0xFFDB2777))),
        Action("📝 Kế hoạch",  Icons.Default.EventNote,       listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))),
        Action("💳 Nợ/Vay",    Icons.Default.AccountBalance,  listOf(Color(0xFF9333EA), Color(0xFF7E22CE))),
        Action("📸 Nhật ký",  Icons.Default.PhotoLibrary,    listOf(Color(0xFFEA580C), Color(0xFFDC2626)))
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { action ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                when {
                                    action.label.contains("Giao dịch") -> onAction(null)
                                    action.label.contains("Tiết kiệm") -> onSavingsAction()
                                    action.label.contains("Ví nhóm")  -> onHeldFundsAction()
                                    action.label.contains("Chuyển")    -> onTransferAction()
                                    action.label.contains("Thống kê")  -> onNavigate(Routes.ANALYTICS)
                                    action.label.contains("Kế hoạch")  -> onNavigate(Routes.BUDGET)
                                    action.label.contains("Nợ")        -> onNavigate(Routes.DEBT_LOAN)
                                    action.label.contains("Nhật ký")  -> onNavigate(Routes.PHOTO_DIARY)
                                }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(action.gradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(action.icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            action.label.substringAfter(" "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun TransactionListItem(transaction: FinanceTransaction, onClick: () -> Unit) {
    val (amtColor, signChar) = when (transaction.type) {
        TransactionType.INCOME            -> Pair(Color(0xFF10B981), "+")
        TransactionType.EXPENSE,
        TransactionType.GROUP_PREPAYMENT  -> Pair(Color(0xFFEF4444), "-")
        TransactionType.TRANSFER          -> Pair(Color(0xFF6366F1), "↔")
    }
    val catIcon = getCategoryIcon(transaction.category)
    val catColor = amtColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon circle with tinted background
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(catIcon, null, tint = catColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.note.ifBlank { transaction.category },
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                transactionDateFormat.format(transaction.timestamp.toDate()),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            )
        }
        Text(
            "$signChar${formatCurrency(if (transaction.isGroupPrepayment) transaction.personalAmount else transaction.amount)}",
            color = amtColor,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
        )
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

