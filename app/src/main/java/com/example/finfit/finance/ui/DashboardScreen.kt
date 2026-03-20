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
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.*
import java.text.*
import java.util.*

// ─── Routes nội bộ ───────────────────────────────────────────
private sealed class Screen {
    object Home : Screen()
    data class EditAccount(val accountId: String?) : Screen()
    data class AddAccount(val dummy: Unit = Unit) : Screen()
    data class EditTransaction(val transaction: FinanceTransaction) : Screen()
}

// ─── Entry point ─────────────────────────────────────────────
@Composable
fun DashboardScreen(
    userEmail: String,
    wallet: AppUserWallet?,
    transactions: List<FinanceTransaction>,
    onSaveWallet: (AppUserWallet) -> Unit,
    onSilentSave: (AppUserWallet) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onUpdateTransaction: (FinanceTransaction) -> Unit,
    onLogout: () -> Unit,
    onAction: (TransactionType?) -> Unit = {}
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
                onSilentSave = onSilentSave,
                onAction = onAction,
                onEditAccount = { id -> screen = Screen.EditAccount(id) },
                onAddAccount = { screen = Screen.AddAccount() },
                onEditTransaction = { tx -> screen = Screen.EditTransaction(tx) }
            )
            is Screen.EditAccount -> EditAccountScreen(
                accountId = s.accountId,
                wallet = wallet,
                onSave = { updated -> onSaveWallet(updated); screen = Screen.Home },
                onDelete = { updated -> onSaveWallet(updated); screen = Screen.Home },
                onBack = { screen = Screen.Home }
            )
            is Screen.AddAccount -> AddAccountScreen(
                wallet = wallet,
                onSave = { updated -> onSaveWallet(updated); screen = Screen.Home },
                onBack = { screen = Screen.Home }
            )
            is Screen.EditTransaction -> EditTransactionScreen(
                transaction = s.transaction,
                wallet = wallet,
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
    onSilentSave: (AppUserWallet) -> Unit,
    onAction: (TransactionType?) -> Unit,
    onEditAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit
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
                            },
                            onClick = { onEditAccount(account.id) }
                        )
                    }
                }
                // Nút thêm tài khoản
                item {
                    AddAccountCard(onClick = onAddAccount)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item { QuickActionsSection(onAction) }
        item { Spacer(Modifier.height(32.dp)) }

        // Mới: Mục tiêu tiết kiệm của bạn
        item { SavingsGoalsSection() }
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
fun SavingsGoalsSection() {
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
                modifier = Modifier.clickable { }
            )
        }
        Spacer(Modifier.height(16.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                GoalCard(
                    title = "Mua xe máy mới",
                    current = 1300.0,
                    target = 2000.0,
                    imageRes = "motorbike_savings_goal" // Giả định dùng tên artifact đã tạo
                )
            }
            item {
                AddGoalCard()
            }
        }
    }
}

@Composable
fun GoalCard(title: String, current: Double, target: Double, imageRes: String) {
    val progress = (current / target).toFloat()
    val progressText = "${(progress * 100).toInt()}%"
    
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Ảnh nền (giả lập bằng placeholder tối hoặc ảnh thực nếu có)
        Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f))) {
             // Ở đây sẽ vẽ ảnh bike (motorbike_savings_goal) nếu link được
        }
        
        // Gradient overlay cho chữ dễ đọc
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "${NumberFormat.getCurrencyInstance(Locale.US).format(current)} / ${NumberFormat.getCurrencyInstance(Locale.US).format(target)}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                    color = Color(0xFF10C67F),
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
                Spacer(Modifier.width(12.dp))
                Text(progressText, color = Color(0xFF10C67F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AddGoalCard() {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .background(Color.Transparent)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Text("Tạo mục tiêu tiết kiệm mới", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

// ─── Màn hình chỉnh sửa giao dịch ───────────────────────────
@Composable
fun EditTransactionScreen(
    transaction: FinanceTransaction,
    wallet: AppUserWallet?,
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
    onToggle: () -> Unit,
    onClick: () -> Unit
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
            .clickable { onClick() }
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

/** Thẻ nút "+ Thêm tài khoản" */
@Composable
fun AddAccountCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(155.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(2.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("Thêm tài khoản", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Màn hình chỉnh sửa tài khoản ───────────────────────────
@Composable
fun EditAccountScreen(
    accountId: String?,
    wallet: AppUserWallet?,
    onSave: (AppUserWallet) -> Unit,
    onDelete: (AppUserWallet) -> Unit,
    onBack: () -> Unit
) {
    if (wallet == null) { onBack(); return }
    val account = wallet.accounts.find { it.id == accountId } ?: run { onBack(); return }

    var editName   by remember { mutableStateOf(account.name) }
    var editAmount by remember {
        mutableStateOf(
            if (account.amount % 1 == 0.0) account.amount.toLong().toString()
            else account.amount.toString()
        )
    }
    var selectedBank  by remember { mutableStateOf(SUPPORTED_BANKS.find { it.code == account.bankCode } ?: SUPPORTED_BANKS.last()) }
    var selectedColor by remember { mutableStateOf(account.colorIndex) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBankPicker   by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa tài khoản?", color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("Bạn có chắc muốn xóa \"${account.displayName}\"?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = wallet.copy(accounts = wallet.accounts.filter { it.id != account.id })
                    onDelete(updated)
                }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showBankPicker) {
        BankPickerDialog(
            onSelected = { bank ->
                selectedBank = bank
                selectedColor = cardGradientIndex(bank.primaryColorHex)
                showBankPicker = false
            },
            onDismiss = { showBankPicker = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Thiết lập tài khoản", color = MaterialTheme.colorScheme.onBackground, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Chọn ngân hàng
        BankSelectorButton(bank = selectedBank, onClick = { showBankPicker = true })

        Spacer(Modifier.height(14.dp))

        // Tên tài khoản
        OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Tên hiển thị (tùy chọn)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground)
        )
        Spacer(Modifier.height(12.dp))

        // Số dư
        OutlinedTextField(
            value = editAmount,
            onValueChange = { editAmount = it },
            label = { Text("Số dư hiện tại") },
            suffix = { Text("đ", color = MaterialTheme.colorScheme.onBackground) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground)
        )

        Spacer(Modifier.height(20.dp))

        // Màu thẻ
        Text("Màu thẻ:", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        ColorPicker(selected = selectedColor, onSelect = { selectedColor = it })

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val amountText = editAmount.replace(".", "").replace(",", "")
                val amount = amountText.toDoubleOrNull() ?: 0.0
                val updated = wallet.copy(
                    accounts = wallet.accounts.map {
                        if (it.id == account.id) it.copy(
                            name       = editName,
                            bankCode   = selectedBank.code,
                            amount     = amount,
                            colorIndex = selectedColor
                        ) else it
                    }
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

// ─── Màn hình thêm tài khoản mới ─────────────────────────────
@Composable
fun AddAccountScreen(
    wallet: AppUserWallet?,
    onSave: (AppUserWallet) -> Unit,
    onBack: () -> Unit
) {
    if (wallet == null) { onBack(); return }

    var editName   by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var selectedBank  by remember { mutableStateOf(SUPPORTED_BANKS[1]) } // MB bank mặc định
    var selectedColor by remember { mutableStateOf(0) }
    var showBankPicker by remember { mutableStateOf(false) }

    if (showBankPicker) {
        BankPickerDialog(
            onSelected = { bank ->
                selectedBank = bank
                selectedColor = cardGradientIndex(bank.primaryColorHex)
                showBankPicker = false
            },
            onDismiss = { showBankPicker = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Thêm tài khoản mới", color = MaterialTheme.colorScheme.onBackground, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        BankSelectorButton(bank = selectedBank, onClick = { showBankPicker = true })
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Tên hiển thị (tùy chọn)") },
            placeholder = { Text("VD: Lương, Tiêu dùng...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = editAmount,
            onValueChange = { editAmount = it },
            label = { Text("Số dư hiện tại") },
            suffix = { Text("đ", color = MaterialTheme.colorScheme.onBackground) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground)
        )

        Spacer(Modifier.height(20.dp))
        Text("Màu thẻ:", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        ColorPicker(selected = selectedColor, onSelect = { selectedColor = it })

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val amountText = editAmount.replace(".", "").replace(",", "")
                val amount = amountText.toDoubleOrNull() ?: 0.0
                val newAccount = AppBankAccount(
                    id         = java.util.UUID.randomUUID().toString(),
                    bankCode   = selectedBank.code,
                    name       = editName,
                    amount     = amount,
                    colorIndex = selectedColor
                )
                onSave(wallet.copy(accounts = wallet.accounts + newAccount))
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Thêm tài khoản", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Dialog chọn ngân hàng ───────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankPickerDialog(onSelected: (BankInfo) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Text(
            "Chọn ngân hàng / ví",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            items(SUPPORTED_BANKS) { bank ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelected(bank) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(bank.primaryColorHex).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(bank.emoji, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(bank.displayName, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─── Nút chọn ngân hàng hiện tại ─────────────────────────────
@Composable
fun BankSelectorButton(bank: BankInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(bank.primaryColorHex).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Text(bank.emoji, fontSize = 22.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Ngân hàng / Ví", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(bank.displayName, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Bộ chọn màu thẻ ─────────────────────────────────────────
@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    val colors = listOf(
        Color(0xFF2D82FE), Color(0xFF10C67F), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFF0EA5E9)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        colors.forEachIndexed { idx, color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(if (idx == selected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                    .clickable { onSelect(idx) },
                contentAlignment = Alignment.Center
            ) {
                if (idx == selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
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
