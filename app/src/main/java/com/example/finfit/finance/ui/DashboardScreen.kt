package com.example.finfit.finance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.BankAccount
import com.example.finfit.finance.model.BankInfo
import com.example.finfit.finance.model.SUPPORTED_BANKS
import com.example.finfit.finance.model.Transaction
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.model.UserWallet
import com.example.finfit.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

// ─── Routes nội bộ ───────────────────────────────────────────
private sealed class Screen {
    object Home : Screen()
    data class EditAccount(val accountId: String?) : Screen()
    data class AddAccount(val dummy: Unit = Unit) : Screen()
    data class EditTransaction(val transaction: Transaction) : Screen()
}

// ─── Entry point ─────────────────────────────────────────────
@Composable
fun DashboardScreen(
    userEmail: String,
    wallet: UserWallet?,
    transactions: List<Transaction>,
    onSaveWallet: (UserWallet) -> Unit,
    onSilentSave: (UserWallet) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onUpdateTransaction: (Transaction) -> Unit,
    onLogout: () -> Unit,
    onAction: (TransactionType?) -> Unit = {}
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val s = screen) {
        is Screen.Home -> HomeContent(
            userEmail   = userEmail,
            wallet      = wallet,
            transactions = transactions,
            onSilentSave = onSilentSave,
            onAction = onAction,
            onEditAccount = { id -> screen = Screen.EditAccount(id) },
            onAddAccount  = { screen = Screen.AddAccount() },
            onEditTransaction = { tx -> screen = Screen.EditTransaction(tx) }
        )
        is Screen.EditAccount -> EditAccountScreen(
            accountId    = s.accountId,
            wallet       = wallet,
            onSave       = { updated -> onSaveWallet(updated); screen = Screen.Home },
            onDelete     = { updated -> onSaveWallet(updated); screen = Screen.Home },
            onBack       = { screen = Screen.Home }
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

// ─── Màn hình chính ──────────────────────────────────────────
@Composable
fun HomeContent(
    userEmail: String,
    wallet: UserWallet?,
    transactions: List<Transaction>,
    onSilentSave: (UserWallet) -> Unit,
    onAction: (TransactionType?) -> Unit,
    onEditAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item { HeaderSection(userEmail) }
        item { Spacer(Modifier.height(20.dp)) }

        // Tổng số dư
        item {
            val total = wallet?.accounts?.sumOf { it.amount } ?: 0.0
            Text("Tổng số dư", color = TextGray, fontSize = 13.sp)
            Text(
                formatCurrency(total),
                color = TextWhite,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        // Danh sách thẻ tài khoản (cuộn ngang)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (wallet != null) {
                    items(wallet.accounts) { account ->
                        AccountCard(
                            account  = account,
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
        item { Spacer(Modifier.height(24.dp)) }
        
        // Hiện tại tạm ẩn phần biểu đồ để tập trung vào list giao dịch
        // item { SpendingBreakdownSection() }
        // item { Spacer(Modifier.height(24.dp)) }
        
        item { RecentTransactionsSection(transactions, onEditTransaction) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─── Recent transactions ───────────────────────
@Composable
fun RecentTransactionsSection(
    transactions: List<Transaction>,
    onEditTransaction: (Transaction) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Giao dịch gần đây", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Xem tất cả", color = PrimaryBlue, fontSize = 13.sp, modifier = Modifier.clickable {  })
        }
        Spacer(Modifier.height(12.dp))
        
        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("Chưa có giao dịch nào", color = TextGray, fontSize = 14.sp)
            }
        } else {
            transactions.forEach { tx ->
                TransactionListItem(
                    icon = getCategoryIcon(tx.category),
                    title = if (tx.note.isNotBlank()) tx.note else tx.category,
                    sub = SimpleDateFormat("HH:mm • dd/MM", Locale.getDefault()).format(tx.timestamp.toDate()) + (if (tx.type == TransactionType.INCOME) " • Thu nhập" else if (tx.type == TransactionType.EXPENSE) " • Chi tiêu" else " • Chuyển tiền"),
                    amount = (if (tx.type == TransactionType.EXPENSE) "-" else "+") + formatCurrency(tx.amount),
                    amountColor = if (tx.type == TransactionType.INCOME) Color(0xFF10C67F) else if (tx.type == TransactionType.EXPENSE) Color(0xFFEF4444) else Color(0xFF6366F1),
                    onClick = { onEditTransaction(tx) }
                )
            }
        }
    }
}

// ─── Màn hình chỉnh sửa giao dịch ───────────────────────────
@Composable
fun EditTransactionScreen(
    transaction: Transaction,
    wallet: UserWallet?,
    onSave: (Transaction) -> Unit,
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
            title = { Text("Xóa giao dịch?", color = TextWhite) },
            text = { Text("Bạn có chắc muốn xóa lịch sử giao dịch này?", color = TextGray) },
            confirmButton = {
                TextButton(onClick = { onDelete(transaction.id) }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy", color = TextGray) }
            },
            containerColor = CardBackground
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
            }
            Text("Chỉnh sửa giao dịch", color = TextWhite, fontSize = 19.sp, fontWeight = FontWeight.Bold)
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
            suffix = { Text("đ", color = TextWhite) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
        )
        Spacer(Modifier.height(16.dp))

        // Ghi chú
        OutlinedTextField(
            value = editNote,
            onValueChange = { editNote = it },
            label = { Text("Ghi chú") },
            placeholder = { Text("Mô tả giao dịch...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
        )
        Spacer(Modifier.height(16.dp))
        
        // Hạng mục (Tạm thời là TextField hiển thị, có thể làm picker sau)
        Text("Hạng mục: $selectedCategory", color = TextGray, fontSize = 14.sp)
        
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
    account: BankAccount,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val bankInfo = SUPPORTED_BANKS.find { it.code == account.bankCode }
        ?: SUPPORTED_BANKS.last()

    val gradientColors = cardGradient(account.colorIndex, bankInfo.primaryColorHex)

    Box(
        modifier = Modifier
            .width(270.dp)
            .height(155.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        // ── Tên ngân hàng + emoji (góc trên trái) ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(bankInfo.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    bankInfo.displayName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    account.name.ifBlank { "Tài khoản" },
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Icon con mắt (góc trên phải) ──
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = if (account.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Ẩn/Hiện số dư",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }

        // ── Số dư (giữa dưới) ──
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = if (account.isHidden) "•••••• đ" else formatCurrency(account.amount),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Số dư khả dụng", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
        }
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
            .background(CardBackground)
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
    wallet: UserWallet?,
    onSave: (UserWallet) -> Unit,
    onDelete: (UserWallet) -> Unit,
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
            title = { Text("Xóa tài khoản?", color = TextWhite) },
            text = { Text("Bạn có chắc muốn xóa \"${account.displayName}\"?", color = TextGray) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = wallet.copy(accounts = wallet.accounts.filter { it.id != account.id })
                    onDelete(updated)
                }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy", color = TextGray) }
            },
            containerColor = CardBackground
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
            }
            Text("Thiết lập tài khoản", color = TextWhite, fontSize = 19.sp, fontWeight = FontWeight.Bold)
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
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
        )
        Spacer(Modifier.height(12.dp))

        // Số dư
        OutlinedTextField(
            value = editAmount,
            onValueChange = { editAmount = it },
            label = { Text("Số dư hiện tại") },
            suffix = { Text("đ", color = TextWhite) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
        )

        Spacer(Modifier.height(20.dp))

        // Màu thẻ
        Text("Màu thẻ:", color = TextWhite, fontWeight = FontWeight.Bold)
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
    wallet: UserWallet?,
    onSave: (UserWallet) -> Unit,
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextWhite)
            }
            Text("Thêm tài khoản mới", color = TextWhite, fontSize = 19.sp, fontWeight = FontWeight.Bold)
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
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = editAmount,
            onValueChange = { editAmount = it },
            label = { Text("Số dư hiện tại") },
            suffix = { Text("đ", color = TextWhite) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite)
        )

        Spacer(Modifier.height(20.dp))
        Text("Màu thẻ:", color = TextWhite, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        ColorPicker(selected = selectedColor, onSelect = { selectedColor = it })

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                val amountText = editAmount.replace(".", "").replace(",", "")
                val amount = amountText.toDoubleOrNull() ?: 0.0
                val newAccount = BankAccount(
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
        containerColor = CardBackground
    ) {
        Text(
            "Chọn ngân hàng / ví",
            color = TextWhite,
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
                    Text(bank.displayName, color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 15.sp)
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
            .background(CardBackground)
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
            Text("Ngân hàng / Ví", color = TextGray, fontSize = 11.sp)
            Text(bank.displayName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Icon(Icons.Default.KeyboardArrowDown, null, tint = TextGray)
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
                modifier = Modifier.size(44.dp).clip(CircleShape).background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Person, null, tint = Color.White) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Xin chào 👋", color = TextGray, fontSize = 12.sp)
                Text(
                    userEmail.substringBefore("@"),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
        Row {
            IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null, tint = TextGray) }
        }
    }
}

// ─── Quick actions ────────────────────────────────────────────
@Composable
fun QuickActionsSection(onAction: (TransactionType?) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ActionItem(Icons.Default.SwapHoriz, "Chuyển tiền") { onAction(TransactionType.TRANSFER) }
        ActionItem(Icons.Default.BarChart,  "Phân tích") { onAction(null) }
        ActionItem(Icons.Default.QrCodeScanner, "Quét QR")   { onAction(null) }
        ActionItem(Icons.Default.MoreHoriz, "Thêm")     { onAction(null) }
    }
}

@Composable
fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(CardBackground),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(label, color = TextGray, fontSize = 11.sp)
    }
}

// ─── Spending breakdown (placeholder) ────────────────────────
@Composable
fun SpendingBreakdownSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Phân tích chi tiêu", color = TextWhite, fontWeight = FontWeight.Bold)
                Text("Tuần này", color = PrimaryBlue, fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(DarkBackground), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("65%", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Đã dùng", color = TextGray, fontSize = 9.sp)
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
        Text(label, color = TextGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("$percent%", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransactionListItem(icon: ImageVector, title: String, sub: String, amount: String, amountColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(CardBackground), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color(0xFFF59E0B))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(sub, color = TextGray, fontSize = 12.sp)
        }
        Text(amount, color = amountColor, fontWeight = FontWeight.Bold)
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
