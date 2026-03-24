package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*

import androidx.activity.compose.BackHandler
import com.example.finfit.core.navigation.Routes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletManagementScreen(
    wallet: AppUserWallet?,
    onSaveWallet: (AppUserWallet) -> Unit,
    onNavigate: (String) -> Unit,
    onHome: () -> Unit = { onNavigate(Routes.DASHBOARD) }
) {
    if (wallet == null) return

    var selectedAccountId by remember { mutableStateOf<String?>(null) }
    var isAddingAccount by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        if (selectedAccountId != null) {
            val id = selectedAccountId!!
            EditAccountScreen(
                accountId = id,
                wallet = wallet,
                onSave = { updatedWallet ->
                    onSaveWallet(updatedWallet)
                    selectedAccountId = null
                },
                onDelete = { updatedWallet ->
                    onSaveWallet(updatedWallet)
                    selectedAccountId = null
                },
                onBack = { selectedAccountId = null },
                onHome = onHome
            )
            BackHandler { selectedAccountId = null }
        } else if (isAddingAccount) {
            AddAccountScreen(
                wallet = wallet,
                onSave = { updatedWallet ->
                    onSaveWallet(updatedWallet)
                    isAddingAccount = false
                },
                onBack = { isAddingAccount = false },
                onHome = onHome
            )
            BackHandler { isAddingAccount = false }
        } else {
            WalletManagementContent(
                wallet = wallet,
                onAddAccount = { isAddingAccount = true },
                onEditAccount = { accountId -> selectedAccountId = accountId },
                onToggleAccount = { account ->
                    val updated = wallet.copy(
                        accounts = wallet.accounts.map {
                            if (it.id == account.id) it.copy(isHidden = !it.isHidden) else it
                        }
                    )
                    onSaveWallet(updated)
                }
            )
        }
    }
}

@Composable
fun WalletManagementContent(
    wallet: AppUserWallet,
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    onToggleAccount: (AppBankAccount) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 30 })
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Quản lý Tài khoản & Ví",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Tổng số dư tài sản từ tất cả các ví của bạn.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Total Balance Card with Gradient
            TotalBalanceDisplayCard(total = wallet.totalBalance)

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "DANH SÁCH TÀI KHOẢN",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onAddAccount) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Thêm mới", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                wallet.accounts.forEachIndexed { index, account ->
                    val itemVisible = remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { itemVisible.value = true }
                    
                    AnimatedVisibility(
                        visible = itemVisible.value,
                        enter = fadeIn(animationSpec = tween(400, delayMillis = index * 100)) + 
                                slideInHorizontally(initialOffsetX = { 20 }, animationSpec = tween(400, delayMillis = index * 100))
                    ) {
                        ManagementAccountCard(
                            account = account,
                            onEdit = { onEditAccount(account.id) },
                            onToggleVisibility = { onToggleAccount(account) }
                        )
                    }
                }
                
                // Add New Card Placeholder
                AddNewAccountPlaceholder(onClick = onAddAccount)
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
fun TotalBalanceDisplayCard(total: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Decorative Circles
            Box(Modifier.offset(x = (-40).dp, y = (-20).dp).size(200.dp).background(Color.White.copy(alpha = 0.05f), CircleShape))
            
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "TỔNG TÀI SẢN KẾT HỢP",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(12.dp))
                AnimatedAmountText(
                    total, 
                    false, 
                    Color.White, 
                    36.sp, 
                    FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Số dư khả dụng để bạn lập kế hoạch chi tiêu.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ManagementAccountCard(
    account: AppBankAccount,
    onEdit: () -> Unit,
    onToggleVisibility: () -> Unit
) {
    val bank = SUPPORTED_BANKS.find { it.code == account.bankCode } ?: SUPPORTED_BANKS.last()
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).graphicsLayer { alpha = if (account.isHidden) 0.5f else 1f },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.Gray.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Text(bank.emoji, fontSize = 28.sp)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatCurrency(account.amount), color = PrimaryBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (account.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null,
                    tint = if (account.isHidden) Color.Gray else PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun AddNewAccountPlaceholder(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircleOutline, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Liên kết tài khoản mới", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 14.sp)
            }
        }
    }
}

// ─── Dialog-like Screens for Add/Edit ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    wallet: AppUserWallet,
    onSave: (AppUserWallet) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var bankCode by remember { mutableStateOf("OTHER") }
    var colorIndex by remember { mutableIntStateOf(0) }
    var showBankPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Thêm tài khoản", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        
        Text("Thông tin cơ bản", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        
        OutlinedCard(
            onClick = { showBankPicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            val bank = SUPPORTED_BANKS.find { it.code == bankCode } ?: SUPPORTED_BANKS.last()
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(bank.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(16.dp))
                Text(bank.displayName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Tên gợi nhớ (VD: ATM chính)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = amount, onValueChange = { if (it.all { c -> c.isDigit() }) amount = it },
            label = { Text("Số dư hiện tại (đ)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                val newAcc = AppBankAccount(
                    id = UUID.randomUUID().toString(),
                    bankCode = bankCode,
                    name = name,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    colorIndex = colorIndex,
                    isHidden = false
                )
                onSave(wallet.copy(accounts = wallet.accounts + newAcc))
            },
            enabled = name.isNotBlank() && amount.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Lưu tài khoản", fontWeight = FontWeight.Bold)
        }
    }

    if (showBankPicker) {
        BankPicker(onSelect = { bankCode = it; showBankPicker = false }, onDismiss = { showBankPicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountScreen(
    accountId: String,
    wallet: AppUserWallet,
    onSave: (AppUserWallet) -> Unit,
    onDelete: (AppUserWallet) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit
) {
    val account = remember(accountId, wallet) { wallet.accounts.find { it.id == accountId } } ?: return
    var name by remember { mutableStateOf(account.name) }
    var amount by remember { mutableStateOf(account.amount.toLong().toString()) }
    var bankCode by remember { mutableStateOf(account.bankCode) }
    var colorIndex by remember { mutableIntStateOf(account.colorIndex) }
    var showBankPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Chỉnh sửa tài khoản", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(24.dp))

        OutlinedCard(onClick = { showBankPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            val bank = SUPPORTED_BANKS.find { it.code == bankCode } ?: SUPPORTED_BANKS.last()
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(bank.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(16.dp))
                Text(bank.displayName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên gợi nhớ") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = amount, onValueChange = { if (it.all { c -> c.isDigit() }) amount = it }, label = { Text("Số dư (đ)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                val updated = wallet.copy(accounts = wallet.accounts.map {
                    if (it.id == accountId) it.copy(bankCode = bankCode, name = name, amount = amount.toDoubleOrNull() ?: 0.0, colorIndex = colorIndex) else it
                })
                onSave(updated)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Lưu thay đổi", fontWeight = FontWeight.Bold) }
        
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = {
            onDelete(wallet.copy(accounts = wallet.accounts.filter { it.id != accountId }))
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Xóa tài khoản này", color = Color.Red.copy(alpha = 0.7f))
        }
    }

    if (showBankPicker) {
        BankPicker(onSelect = { bankCode = it; showBankPicker = false }, onDismiss = { showBankPicker = false })
    }
}

@Composable
fun BankPicker(onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn ngân hàng/ví", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.height(400.dp)) {
                items(SUPPORTED_BANKS) { bank ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(bank.code) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(bank.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(16.dp))
                        Text(bank.displayName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
        shape = RoundedCornerShape(24.dp)
    )
}
