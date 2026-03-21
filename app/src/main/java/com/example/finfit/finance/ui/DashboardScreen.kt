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
                onNavigate = onNavigate
            )
            is Screen.EditTransaction -> EditTransactionScreen(
                transaction = s.transaction,
                onSave = { updated -> onUpdateTransaction(updated); screen = Screen.Home },
                onDelete = { id -> onDeleteTransaction(id); screen = Screen.Home },
                onBack = { screen = Screen.Home }
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
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { HeaderSection(userEmail) }
        item { Spacer(Modifier.height(20.dp)) }

        // Tổng số dư
        item {
            var isTotalHidden by remember { mutableStateOf(true) }
            val total = wallet?.accounts?.sumOf { it.amount } ?: 0.0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Tổng tài chính",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                    IconButton(
                        onClick = { isTotalHidden = !isTotalHidden },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isTotalHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Balance",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isTotalHidden) "****" else formatCurrency(total),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Danh sách thẻ tài khoản (cuộn ngang)
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (wallet != null) {
                    items(wallet.accounts) { account ->
                        AccountCard(
                            account = account,
                            onToggle = {
                                val updated = wallet.copy(
                                    accounts = wallet.accounts.map {
                                        if (it.id == account.id) it.copy(isHidden = !it.isHidden) else it
                                    }
                                )
                                onSilentSave(updated)
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item { QuickActionsSection(onAction) }
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
            transactions.forEach { tx ->
                TransactionListItem(
                    transaction = tx,
                    onClick = { onEditTransaction(tx) }
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
    onBack: () -> Unit
) {
    var editAmount by remember { mutableStateOf(if (transaction.amount % 1 == 0.0) transaction.amount.toLong().toString() else transaction.amount.toString()) }
    var editNote   by remember { mutableStateOf(transaction.note) }
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Chỉnh sửa giao dịch", color = MaterialTheme.colorScheme.onBackground, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(24.dp))

        // Số tiền
        OutlinedTextField(
            value = editAmount,
            onValueChange = { editAmount = it },
            label = { Text("Số tiền") },
            suffix = { Text("đ", color = MaterialTheme.colorScheme.onBackground) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground)
        )
        Spacer(Modifier.height(16.dp))

        // Ghi chú
        OutlinedTextField(
            value = editNote,
            onValueChange = { editNote = it },
            label = { Text("Ghi chú") },
            placeholder = { Text("Mô tả giao dịch...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground)
        )
        Spacer(Modifier.height(16.dp))
        
        // Hạng mục (Tạm thời là TextField hiển thị, có thể làm picker sau)
        Text("Hạng mục: $selectedCategory", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        
        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val amountText = editAmount.replace(".", "").replace(",", "")
                val amount = amountText.toDoubleOrNull() ?: 0.0
                val updated = transaction.copy(
                    amount = amount,
                    note = editNote,
                    category = selectedCategory
                )
                onSave(updated)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
    onToggle: () -> Unit
) {
    val bankInfo = SUPPORTED_BANKS.find { it.code == account.bankCode }
        ?: SUPPORTED_BANKS.last()

    val gradientColors = cardGradient(account.colorIndex, bankInfo.primaryColorHex)

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
                    imageVector = if (account.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
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
                text = if (account.isHidden) "********" else formatCurrency(account.amount),
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
fun QuickActionsSection(onAction: (TransactionType?) -> Unit) {
    val actions = listOf(
        Triple("Thu nhập", Icons.Default.TrendingUp, Color(0xFF10B981)),
        Triple("Chi tiêu", Icons.Default.TrendingDown, Color(0xFFEF4444)),
        Triple("Chuyển tiền", Icons.AutoMirrored.Filled.CompareArrows, Color(0xFF6366F1)),
        Triple("Thống kê", Icons.Default.PieChart, Color(0xFFF59E0B))
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        actions.forEach { (label, icon, color) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { 
                        val type = when(label) {
                            "Thu nhập" -> TransactionType.INCOME
                            "Chi tiêu" -> TransactionType.EXPENSE
                            "Chuyển tiền" -> TransactionType.TRANSFER
                            else -> null
                        }
                        onAction(type)
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

// ─── Helper functions ─────────────────────────────────────────
fun formatCurrency(amount: Double): String {
    val fmt = NumberFormat.getInstance(Locale("vi", "VN"))
    fmt.maximumFractionDigits = 0
    return "${fmt.format(amount)} đ"
}

/** Trả về danh sách màu gradient tương ứng với chỉ số màu */
fun cardGradient(colorIndex: Int, bankColorHex: Long): List<Color> {
    val presets = listOf(
        listOf(Color(0xFF2D82FE), Color(0xFF1E40AF)),
        listOf(Color(0xFF10C67F), Color(0xFF065F46)),
        listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
        listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6)),
        listOf(Color(0xFFEF4444), Color(0xFF991B1B)),
        listOf(Color(0xFF0EA5E9), Color(0xFF0369A1)),
    )
    return presets.getOrElse(colorIndex) {
        val base = Color(bankColorHex)
        listOf(base, base.copy(red = (base.red * 0.7f).coerceIn(0f, 1f)))
    }
}

/** Chuyển màu hex ngân hàng → chỉ số thẻ màu gần nhất */
fun cardGradientIndex(bankColorHex: Long): Int {
    return when (bankColorHex) {
        0xFF059669L, 0xFF007A33L, 0xFF006838L, 0xFF00A651L, 0xFF009B4DL -> 1 // green
        0xFFF59E0B -> 2   // orange
        0xFFAE1F7EL, 0xFF6B21A8L -> 3  // purple
        0xFFE31837L, 0xFFDC2626L -> 4  // red
        0xFF0068FFL -> 5  // light blue
        else -> 0         // default blue
    }
}
