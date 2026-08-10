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
    goalsTotal: Double = 0.0,
    totalDebts: Double = 0.0,
    totalLoans: Double = 0.0,
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
                goalsTotal = goalsTotal,
                totalDebts = totalDebts,
                totalLoans = totalLoans,
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
    goalsTotal: Double = 0.0,
    totalDebts: Double = 0.0,
    totalLoans: Double = 0.0,
    onAddAccount: () -> Unit,
    onEditAccount: (String) -> Unit,
    onToggleAccount: (AppBankAccount) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val netWorth = remember(wallet, goalsTotal, totalDebts, totalLoans) {
        wallet.calculateNetWorth(goalsTotal, totalDebts, totalLoans)
    }

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
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                "Phân loại tài khoản theo mục đích và theo dõi tổng tài sản ròng.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Net Worth Card (Tài sản ròng)
            NetWorthDisplayCard(
                netWorth = netWorth,
                totalBalance = wallet.totalBalance,
                savings = wallet.generalSavings + goalsTotal,
                loans = totalLoans + wallet.totalGroupPrepaid,
                debts = totalDebts
            )

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "TÀI KHOẢN THEO MỤC ĐÍCH",
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

            Spacer(Modifier.height(8.dp))

            // Gom nhóm tài khoản theo mục đích sử dụng
            AccountPurpose.values().forEach { purpose ->
                val accountsInPurpose = wallet.accounts.filter { it.purpose == purpose }
                if (accountsInPurpose.isNotEmpty()) {
                    val groupTotal = accountsInPurpose.sumOf { it.amount }
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(purpose.iconEmoji, fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(purpose.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(formatCurrency(groupTotal), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                        }

                        Spacer(Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            accountsInPurpose.forEach { account ->
                                ManagementAccountCard(
                                    account = account,
                                    onEdit = { onEditAccount(account.id) },
                                    onToggleVisibility = { onToggleAccount(account) }
                                )
                            }
                        }
                    }
                }
            }

            // Nếu chưa có tài khoản nào thuộc nhóm nào
            if (wallet.accounts.isEmpty()) {
                AddNewAccountPlaceholder(onClick = onAddAccount)
            } else {
                Spacer(Modifier.height(12.dp))
                AddNewAccountPlaceholder(onClick = onAddAccount)
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

// ─── Net Worth Card ─────────────────────────────────────────────────────────

@Composable
fun NetWorthDisplayCard(
    netWorth: Double,
    totalBalance: Double,
    savings: Double,
    loans: Double,
    debts: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.offset(x = (-40).dp, y = (-20).dp).size(200.dp).background(Color.White.copy(alpha = 0.05f), CircleShape))
            
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "TỔNG TÀI SẢN RÒNG (NET WORTH)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(8.dp))
                AnimatedAmountText(
                    netWorth, 
                    false, 
                    Color.White, 
                    32.sp, 
                    FontWeight.Black
                )
                Spacer(Modifier.height(16.dp))
                
                // Sub-breakdown pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Tài sản (Ví + Tiết kiệm)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(formatCurrency(totalBalance + savings + loans), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Tổng nợ phải trả", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(formatCurrency(debts), fontSize = 12.sp, color = if (debts > 0) Color(0xFFFF8A80) else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
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
    val isLowBalance = account.isLowBalance

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isLowBalance) Color(0xFFEF4444).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.graphicsLayer { alpha = if (account.isHidden) 0.5f else 1f },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.Gray.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(bank.emoji, fontSize = 24.sp)
                }
                
                Spacer(Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(account.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (account.accountNumber.isNotBlank()) {
                        Text("STK: ${account.accountNumber}", fontSize = 11.sp, color = Color.Gray)
                    }
                    if (account.isHidden) {
                        Text("•••••• đ", color = Color.Gray, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    } else {
                        Text(formatCurrency(account.amount), color = PrimaryBlue, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
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

            if (isLowBalance) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cảnh báo: Số dư dưới ngưỡng an toàn (${formatCurrency(account.lowBalanceThreshold)})", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddNewAccountPlaceholder(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircleOutline, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Liên kết tài khoản mới", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 13.sp)
            }
        }
    }
}

// ─── Add / Edit Account Screen ───────────────────────────────────────────────

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
    var accountNumber by remember { mutableStateOf("") }
    var bankCode by remember { mutableStateOf("OTHER") }
    var purpose by remember { mutableStateOf(AccountPurpose.DAILY_SPENDING) }
    var thresholdText by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }
    var showBankPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Thêm tài khoản", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(20.dp))
        
        Text("Ngân hàng / Loại tài khoản", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        
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
        Spacer(Modifier.height(14.dp))
        
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Tên gợi nhớ (VD: ATM chính)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = accountNumber, onValueChange = { accountNumber = it },
            label = { Text("Số tài khoản (để tạo VietQR nhận tiền)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        Spacer(Modifier.height(14.dp))
        
        VnAmountTextField(
            rawValue = amount,
            onValueChange = { amount = it },
            label = "Số dư hiện tại (đ)",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        // Chọn mục đích tài khoản
        Text("Mục đích sử dụng tài khoản", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AccountPurpose.values().forEach { p ->
                val isSelected = purpose == p
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { purpose = p }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { purpose = p })
                    Spacer(Modifier.width(8.dp))
                    Text("${p.iconEmoji} ${p.displayName}", fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        VnAmountTextField(
            rawValue = thresholdText,
            onValueChange = { thresholdText = it },
            label = "Ngưỡng cảnh báo số dư thấp (đ - tùy chọn)",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                val newAcc = AppBankAccount(
                    id = UUID.randomUUID().toString(),
                    bankCode = bankCode,
                    name = name,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    colorIndex = colorIndex,
                    isHidden = false,
                    purpose = purpose,
                    lowBalanceThreshold = thresholdText.toDoubleOrNull() ?: 0.0,
                    accountNumber = accountNumber
                )
                onSave(wallet.copy(accounts = wallet.accounts + newAcc))
            },
            enabled = name.isNotBlank() && amount.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
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
    var accountNumber by remember { mutableStateOf(account.accountNumber) }
    var bankCode by remember { mutableStateOf(account.bankCode) }
    var purpose by remember { mutableStateOf(account.purpose) }
    var thresholdText by remember { mutableStateOf(if (account.lowBalanceThreshold > 0) account.lowBalanceThreshold.toLong().toString() else "") }
    var colorIndex by remember { mutableIntStateOf(account.colorIndex) }
    var showBankPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Chỉnh sửa tài khoản", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(20.dp))

        OutlinedCard(onClick = { showBankPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            val bank = SUPPORTED_BANKS.find { it.code == bankCode } ?: SUPPORTED_BANKS.last()
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(bank.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(16.dp))
                Text(bank.displayName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên gợi nhớ") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = accountNumber, onValueChange = { accountNumber = it },
            label = { Text("Số tài khoản (để tạo VietQR nhận tiền)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(14.dp))
        VnAmountTextField(
            rawValue = amount,
            onValueChange = { amount = it },
            label = "Số dư (đ)",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Text("Mục đích sử dụng tài khoản", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AccountPurpose.values().forEach { p ->
                val isSelected = purpose == p
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { purpose = p }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { purpose = p })
                    Spacer(Modifier.width(8.dp))
                    Text("${p.iconEmoji} ${p.displayName}", fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        VnAmountTextField(
            rawValue = thresholdText,
            onValueChange = { thresholdText = it },
            label = "Ngưỡng cảnh báo số dư thấp (đ)",
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                val updated = wallet.copy(accounts = wallet.accounts.map {
                    if (it.id == accountId) it.copy(
                        bankCode = bankCode,
                        name = name,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        colorIndex = colorIndex,
                        purpose = purpose,
                        lowBalanceThreshold = thresholdText.toDoubleOrNull() ?: 0.0,
                        accountNumber = accountNumber
                    ) else it
                })
                onSave(updated)
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
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
