package com.example.finfit.finance.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
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

// ─── Routes nội bộ ───────────────────────────────────────────
private sealed class Screen {
    object Home : Screen()
    data class EditTransaction(val transaction: FinanceTransaction) : Screen()
}

// ─── Entry point ─────────────────────────────────────────────
@Composable
fun DashboardScreen(
    userEmail: String,
    wallet: AppUserWallet?,
    transactions: List<FinanceTransaction>,
    goals: List<SavingsGoal>,
    onSilentSave: (AppUserWallet) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onUpdateTransaction: (FinanceTransaction) -> Unit,
    onAction: (TransactionType?) -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        when (val s = screen) {
            is Screen.Home -> HomeContent(
                userEmail = userEmail,
                wallet = wallet,
                transactions = transactions,
                goals = goals,
                onSilentSave = onSilentSave,
                onAction = onAction,
                onEditTransaction = { tx -> screen = Screen.EditTransaction(tx) },
                onNavigate = onNavigate,
                onSavingsAction = { /* This callback is not strictly necessary if handled in HomeContent */ }
            )
            is Screen.EditTransaction -> EditTransactionScreen(
                transaction = s.transaction,
                onSave = { updated -> onUpdateTransaction(updated); screen = Screen.Home },
                onDelete = { id -> onDeleteTransaction(id); screen = Screen.Home },
                onBack = { screen = Screen.Home },
                onHome = { screen = Screen.Home }
            )
        }
    }
}

// ─── Màn hình chính ──────────────────────────────────────────
@Composable
fun HomeContent(
    userEmail: String,
    wallet: AppUserWallet?,
    transactions: List<FinanceTransaction>,
    goals: List<SavingsGoal>,
    onSilentSave: (AppUserWallet) -> Unit,
    onAction: (TransactionType?) -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onNavigate: (String) -> Unit,
    onSavingsAction: (() -> Unit)? = null
) {
    var isDashboardHidden by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { HeaderSection(userEmail) }
        item { Spacer(Modifier.height(20.dp)) }

        // Fund Distribution Section (4-part Money Management)
        item {
            // Tính toán bằng remember để tránh jank khi scroll
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
                onToggleVisible = { isDashboardHidden = !isDashboardHidden }
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Danh sách thẻ tài khoản (cuộn ngang)
        item {
            val accounts = remember(wallet?.accounts) { wallet?.accounts ?: emptyList() }
            
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = accounts,
                    key = { it.id } // Quan trọng để LazyRow scroll mượt
                ) { account ->
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
        item { Spacer(Modifier.height(32.dp)) }

        // Mới: Mục tiêu tiết kiệm của bạn
        item { SavingsGoalsSection(goals, onNavigate) }
        item { Spacer(Modifier.height(32.dp)) }
        
        item { RecentTransactionsSection(transactions, onEditTransaction) }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

// ─── Recent transactions ───────────────────────
@Composable
fun RecentTransactionsSection(
    transactions: List<FinanceTransaction>,
    onEditTransaction: (FinanceTransaction) -> Unit
) {
    var showAll by remember { mutableStateOf(false) }
    
    // Sắp xếp giảm dần theo thời gian và lấy dữ liệu
    val sortedTransactions = remember(transactions) {
        transactions.sortedByDescending { it.timestamp }
    }
    
    val transactionsToDisplay = if (showAll) sortedTransactions else sortedTransactions.take(3)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hoạt động gần đây",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = (-0.5).sp
            )
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        
        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("Chưa có giao dịch nào", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            transactionsToDisplay.forEach { tx ->
                TransactionListItem(
                    transaction = tx,
                    onClick = { onEditTransaction(tx) }
                )
            }
            
            if (transactions.size > 3) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (showAll) "Thu gọn" else "Xem thêm ${transactions.size - 3} giao dịch",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAll = !showAll }
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


// ─── Savings Goals (New for Image 3) ───────────────────────
@Composable
fun SavingsGoalsSection(goals: List<SavingsGoal>, onNavigate: (String) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mục tiêu tiết kiệm của bạn",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Xem tất cả",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigate(Routes.SAVINGS_GOALS) }
            )
        }
        Spacer(Modifier.height(16.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(goals) { goal ->
                GoalCard(
                    goal = goal,
                    onClick = { onNavigate(Routes.SAVINGS_GOALS) }
                )
            }
            item {
                AddGoalCard(onClick = { onNavigate(Routes.SAVINGS_GOALS) })
            }
        }
    }
}

@Composable
fun GoalCard(goal: SavingsGoal, onClick: () -> Unit) {
    val progress = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(goal.colorHex).copy(alpha = 0.1f))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
             Row(verticalAlignment = Alignment.CenterVertically) {
                 Box(
                     modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(goal.colorHex).copy(alpha = 0.2f)),
                     contentAlignment = Alignment.Center
                 ) {
                     Text(goal.iconEmoji, fontSize = 20.sp)
                 }
                 Spacer(Modifier.width(12.dp))
                 Text(goal.goalName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
             }
             
             Column {
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                     Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(goal.colorHex))
                     Text("${formatCurrency(goal.currentAmount)} / ${formatCurrency(goal.targetAmount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                 }
                 Spacer(Modifier.height(8.dp))
                 LinearProgressIndicator(
                     progress = { progress },
                     modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                     color = Color(goal.colorHex),
                     trackColor = Color(goal.colorHex).copy(alpha = 0.1f),
                     strokeCap = StrokeCap.Round
                 )
             }
        }
    }
}
@Composable
fun AddGoalCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .background(Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Thêm mục tiêu mới",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Màn hình chỉnh sửa giao dịch ───────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transaction: FinanceTransaction,
    onSave: (FinanceTransaction) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack
) {
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa giao dịch?", color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("Bạn có chắc muốn xóa lịch sử giao dịch này?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { onDelete(transaction.id) }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Chi tiết giao dịch", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onHome) {
                Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // --- Hiển thị thông tin nguyên bản (Khóa chỉnh sửa) ---
        Text(
            "THÔNG TIN GIAO DỊCH",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = formatCurrency(transaction.amount),
            onValueChange = {},
            label = { Text("Số tiền") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(14.dp)
        )
        
        Spacer(Modifier.height(12.dp))
        
        OutlinedTextField(
            value = transaction.note.ifBlank { "Không có nội dung" },
            onValueChange = {},
            label = { Text("Nội dung gốc") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(14.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        // --- Chỉnh sửa hạng mục ---
        Text(
            "PHÂN LOẠI",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Sử dụng CategoryPicker từ logic tương đương AddTransactionScreen
        val categories = if (transaction.type == TransactionType.INCOME) INCOME_CATEGORIES else EXPENSE_CATEGORIES
        categories.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { cat ->
                    val isSelected = selectedCategory == cat.label
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) cat.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                            .border(
                                if (isSelected) 1.5.dp else 0.dp,
                                if (isSelected) cat.color else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = cat.label }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(cat.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.height(4.dp))
                        Text(cat.label, color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                val updated = transaction.copy(category = selectedCategory)
                onSave(updated)
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Xác nhận phân loại", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return EXPENSE_CATEGORIES.find { it.label == category }?.icon 
        ?: INCOME_CATEGORIES.find { it.label == category }?.icon 
        ?: Icons.Default.Receipt
}

// ─── Thẻ tài khoản ───────────────────────────────────────────
@Composable
fun AccountCard(
    account: AppBankAccount, 
    isHiddenGlobal: Boolean,
    onToggle: () -> Unit
) {
    val bankInfo = remember(account.bankCode) {
        SUPPORTED_BANKS.find { it.code == account.bankCode } ?: SUPPORTED_BANKS.last()
    }
    
    val isActuallyHidden = isHiddenGlobal || account.isHidden
    val gradientColors = remember(account.colorIndex, bankInfo.primaryColorHex) {
        cardGradient(account.colorIndex, bankInfo.primaryColorHex)
    }

    Box(
        modifier = Modifier
            .width(300.dp)
            .height(170.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(gradientColors))
            .padding(20.dp)
    ) {
        // Góc trên: Tên ngân hàng và Nút ẩn/hiện
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bankInfo.emoji,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = bankInfo.displayName.uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            
            IconButton(
                onClick = { onToggle() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isActuallyHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Balance",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Giữa: Số dư
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = "Số dư khả dụng",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isActuallyHidden) "********" else formatCurrency(account.amount),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        // Dưới cùng: Tên hiển thị
        Text(
            text = account.name.ifBlank { "CHỦ TÀI KHOẢN" }.uppercase(),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomStart),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ─── Header ──────────────────────────────────────────────────
@Composable
fun HeaderSection(userEmail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Xin chào 👋", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(
                    userEmail.substringBefore("@"),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.5).sp
                )
            }
        }
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
        }
    }
}

// ─── Quick Actions ──────────────────────────
@Composable
fun QuickActionsSection(
    onAction: (TransactionType?) -> Unit,
    onSavingsAction: () -> Unit,
    onHeldFundsAction: () -> Unit,
    onTransferAction: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val actions = remember {
        listOf(
            Triple("Giao dịch", Icons.Default.Add, PrimaryBlue),
            Triple("Tiết kiệm", Icons.Default.Savings, Color(0xFF10B981)),
            Triple("Ví nhóm", Icons.Default.Groups, Color(0xFFF59E0B)),
            Triple("Chuyển tiền", Icons.Default.SwapHoriz, Color(0xFF6366F1)),
            Triple("Thống kê", Icons.Default.BarChart, Color(0xFFEC4899)),
            Triple("Kế hoạch", Icons.Default.EventNote, Color(0xFF8B5CF6)),
            Triple("Nợ/Vay", Icons.Default.AccountBalance, Color(0xFF9333EA))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        actions.chunked(4).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowActions.forEach { (label, icon, color) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { 
                                when(label) {
                                    "Giao dịch" -> onAction(null)
                                    "Tiết kiệm" -> onSavingsAction()
                                    "Ví nhóm" -> onHeldFundsAction()
                                    "Chuyển tiền" -> onTransferAction()
                                    "Thống kê" -> onNavigate(Routes.ANALYTICS)
                                    "Kế hoạch" -> onNavigate(Routes.BUDGET)
                                    "Nợ/Vay" -> onNavigate(Routes.DEBT_LOAN) // Assuming a new route for Debt/Loan
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = color,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Thêm các ô trống nếu hàng không đủ 4 phần tử để giữ layout cân đối
                if (rowActions.size < 4) {
                    repeat(4 - rowActions.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─── Spending breakdown (placeholder) ────────────────────────
@Composable
fun SpendingBreakdownSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Phân tích chi tiêu", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                Text("Tuần này", color = PrimaryBlue, fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("65%", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Đã dùng", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    SpendingRow("Ăn uống", 35, Color(0xFFF59E0B))
                    Spacer(Modifier.height(8.dp))
                    SpendingRow("Di chuyển", 20, Color(0xFF8B5CF6))
                    Spacer(Modifier.height(8.dp))
                    SpendingRow("Mua sắm", 10, Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun SpendingRow(label: String, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("$percent%", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── TransactionListItem ───────────────────────
@Composable
fun TransactionListItem(transaction: FinanceTransaction, onClick: () -> Unit) {
    val amountPrefix = if (transaction.type == TransactionType.EXPENSE) "-" else "+"
    val amountColor = if (transaction.type == TransactionType.INCOME) Color(0xFF10C67F) else if (transaction.type == TransactionType.EXPENSE) Color(0xFFEF4444) else Color(0xFF6366F1)
    val timeStr = SimpleDateFormat("HH:mm • dd/MM", Locale.getDefault()).format(transaction.timestamp.toDate())
    val typeStr = when(transaction.type) {
        TransactionType.INCOME -> "Thu nhập"
        TransactionType.EXPENSE -> "Thanh toán"
        else -> "Điều chuyển"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                getCategoryIcon(transaction.category),
                null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (transaction.note.isNotBlank()) transaction.note else transaction.category,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = "$typeStr • $timeStr",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        
        Text(
            text = "$amountPrefix${formatCurrency(transaction.amount)}",
            color = amountColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

// ─── Sections ───────────────────────────────────────────────

@Composable
fun FundDistributionSection(
    totalManaged: Double,
    personalMoney: Double,
    spendable: Double,
    goalCommitted: Double,
    generalSaved: Double,
    heldFunds: Double,
    onAdjustGeneral: () -> Unit,
    isHiddenGlobal: Boolean,
    onToggleVisible: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "TÀI SẢN CÁ NHÂN",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isHiddenGlobal) "********" else formatCurrency(personalMoney),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (!isHiddenGlobal && heldFunds > 0) {
                        Text(
                            "Tổng tiền đang quản lý: ${formatCurrency(totalManaged)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onToggleVisible) {
                    Icon(
                        if (isHiddenGlobal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Distribution Progress Bar: Tính tỷ lệ dựa trên TỔNG TIỀN ĐANG QUẢN LÝ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                val base = totalManaged.coerceAtLeast(1.0)
                val spendWeight = (spendable / base).toFloat()
                val goalWeight = (goalCommitted / base).toFloat()
                val generalWeight = (generalSaved / base).toFloat()
                val heldWeight = (heldFunds / base).toFloat()

                if (spendWeight > 0) Box(Modifier.fillMaxHeight().weight(spendWeight).background(PrimaryBlue))
                if (goalWeight > 0) Box(Modifier.fillMaxHeight().weight(goalWeight).background(Color(0xFFF59E0B)))
                if (generalWeight > 0) Box(Modifier.fillMaxHeight().weight(generalWeight).background(AccentGreen))
                if (heldWeight > 0) Box(Modifier.fillMaxHeight().weight(heldWeight).background(Color(0xFF8B5CF6)))
            }

            Spacer(Modifier.height(20.dp))

            // Breakdown items
            FundItem(
                label = "Sử dụng thoải mái",
                amount = spendable,
                color = PrimaryBlue,
                icon = Icons.Default.AccountBalanceWallet,
                isHidden = isHiddenGlobal
            )
            Spacer(Modifier.height(12.dp))
            FundItem(
                label = "Tiết kiệm mục tiêu",
                amount = goalCommitted,
                color = Color(0xFFF59E0B),
                icon = Icons.Default.TrendingUp,
                isHidden = isHiddenGlobal
            )
            Spacer(Modifier.height(12.dp))
            FundItem(
                label = "Tiết kiệm chung (Dự phòng)",
                amount = generalSaved,
                color = AccentGreen,
                icon = Icons.Default.Lock,
                isHidden = isHiddenGlobal
            )
            Spacer(Modifier.height(12.dp))
            FundItem(
                label = "Tiền giữ hộ (Quỹ nhóm)",
                amount = heldFunds,
                color = Color(0xFF8B5CF6),
                icon = Icons.Default.Groups,
                isHidden = isHiddenGlobal
            )
        }
    }
}

@Composable
fun FundItem(
    label: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    isHidden: Boolean,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isHidden) "****" else formatCurrency(amount),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
