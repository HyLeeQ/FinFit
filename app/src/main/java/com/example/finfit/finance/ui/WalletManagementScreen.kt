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
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.activity.compose.BackHandler
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletManagementScreen(
    wallet: AppUserWallet?,
    onSaveWallet: (AppUserWallet) -> Unit,
    onNavigate: (String) -> Unit
) {
    if (wallet == null) return

    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var isAddingAccount by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        if (selectedAccountId != null) {
            EditAccountScreen(
                accountId = selectedAccountId,
                wallet = wallet,
                onSave = { updatedWallet ->
                    onSaveWallet(updatedWallet)
                    selectedAccountId = null
                },
                onDelete = { updatedWallet ->
                    onSaveWallet(updatedWallet)
                    selectedAccountId = null
                },
                onBack = { selectedAccountId = null }
            )
            BackHandler { selectedAccountId = null }
        } else if (isAddingAccount) {
            AddAccountScreen(
                wallet = wallet,
                onSave = { updatedWallet ->
                    onSaveWallet(updatedWallet)
                    isAddingAccount = false
                },
                onBack = { isAddingAccount = false }
            )
            BackHandler { isAddingAccount = false }
        } else {
            WalletManagementContent(
                wallet = wallet,
                onAddAccount = { isAddingAccount = true },
                onEditAccount = { accountId -> selectedAccountId = accountId }
            )
        }
    }
}

@Composable
fun WalletManagementContent(
    wallet: AppUserWallet,
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Quản lý Ví & Tài khoản",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ValuableSummaryCard(wallet = wallet)

        Spacer(Modifier.height(24.dp))

        Text(
            "Danh sách tài khoản",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            wallet.accounts.forEach { account ->
                LinkedAccountCard(
                    account = account,
                    onClick = { onEditAccount(account.id) },
                    onToggle = {}
                )
            }
            AddAccountCard(onClick = onAddAccount)
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun ValuableSummaryCard(wallet: AppUserWallet) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2D82FE), Color(0xFF0068FF))))
            .padding(24.dp)
    ) {
        Column {
            Text(
                "Tổng tài sản",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatCurrency(wallet.totalBalance),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// ─── Tái sử dụng các thẻ và dialog từ DashboardScreen ─────────
// Để module hóa tốt hơn, tôi đã sao chép/tinh chỉnh lại ở đây cho phần Quản lý Ví

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
    var editAmount by remember { mutableStateOf(if (account.amount % 1 == 0.0) account.amount.toLong().toString() else account.amount.toString()) }
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
        BankSelectorButton(bank = selectedBank, onClick = { showBankPicker = true })
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Tên hiển thị (tùy chọn)") },
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
            Text("Lưu tài khoản", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AddAccountScreen(
    wallet: AppUserWallet,
    onSave: (AppUserWallet) -> Unit,
    onBack: () -> Unit
) {
    var editName   by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var selectedBank  by remember { mutableStateOf(SUPPORTED_BANKS.run { find { it.code == "OTHER" } ?: last() }) }
    var selectedColor by remember { mutableStateOf(cardGradientIndex(selectedBank.primaryColorHex)) }
    var showBankPicker   by remember { mutableStateOf(false) }

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
            label = { Text("Số dư ban đầu") },
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
                    id         = UUID.randomUUID().toString(),
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

@Composable
fun LinkedAccountCard(account: AppBankAccount, onClick: () -> Unit, onToggle: () -> Unit) {
    val bankInfo = SUPPORTED_BANKS.find { it.code == account.bankCode } ?: SUPPORTED_BANKS.last()
    
    val presets = listOf(
        listOf(Color(0xFF2D82FE), Color(0xFF0068FF)),
        listOf(Color(0xFF10C67F), Color(0xFF0EA5E9)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
        listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
        listOf(Color(0xFFEF4444), Color(0xFFE31837)),
        listOf(Color(0xFF0EA5E9), Color(0xFF3B82F6))
    )
    val colorPair = presets.getOrElse(account.colorIndex) { presets[0] }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colorPair))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(bankInfo.emoji, fontSize = 16.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = bankInfo.displayName,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
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

@Composable
fun AddAccountCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text("Thêm tài khoản / ví mới", color = PrimaryBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// formatCurrency will be resolved from DashboardScreen.kt or should be moved to a Utils.kt
