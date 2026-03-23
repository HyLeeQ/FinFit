package com.example.finfit.finance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.AppBankAccount
import com.example.finfit.finance.model.AppUserWallet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalTransferScreen(
    wallet: AppUserWallet,
    onNavigateBack: () -> Unit,
    onConfirmTransfer: (fromAccountId: String, toAccountId: String, amount: Double, note: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("Chuyển tiền nội bộ") }
    
    val accounts = wallet.accounts
    var fromAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var toAccount by remember { mutableStateOf(accounts.getOrNull(1) ?: accounts.firstOrNull()) }
    
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chuyển khoản nội bộ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Transfer Logic Visualization
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TransferAccountBox(
                    label = "Từ tài khoản",
                    account = fromAccount,
                    onClick = { showFromPicker = true }
                )
                
                Icon(
                    Icons.AutoMirrored.Filled.CompareArrows, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                TransferAccountBox(
                    label = "Đến tài khoản",
                    account = toAccount,
                    onClick = { showToPicker = true }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                label = { Text("Số tiền muốn chuyển") },
                prefix = { Text("₫ ", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ghi chú") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val fromId = fromAccount?.id ?: ""
                    val toId = toAccount?.id ?: ""
                    if (amt > 0 && fromId.isNotEmpty() && toId.isNotEmpty() && fromId != toId) {
                        onConfirmTransfer(fromId, toId, amt, note)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = amount.isNotEmpty() && fromAccount?.id != toAccount?.id
            ) {
                Text("Xác nhận chuyển tiền", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal Pickers
    if (showFromPicker) {
        AccountPickerSheet(
            title = "Chọn tài khoản nguồn",
            accounts = accounts,
            onSelect = { fromAccount = it; showFromPicker = false },
            onDismiss = { showFromPicker = false }
        )
    }

    if (showToPicker) {
        AccountPickerSheet(
            title = "Chọn tài khoản đích",
            accounts = accounts,
            onSelect = { toAccount = it; showToPicker = false },
            onDismiss = { showToPicker = false }
        )
    }
}

@Composable
fun TransferAccountBox(
    label: String,
    account: AppBankAccount?,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.size(100.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    account?.name ?: "Chọn thẻ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    title: String,
    accounts: List<AppBankAccount>,
    onSelect: (AppBankAccount) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp).padding(horizontal = 20.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            accounts.forEach { account ->
                ListItem(
                    headlineContent = { Text(account.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(formatCurrency(account.amount)) },
                    leadingContent = {
                        Icon(Icons.Default.CreditCard, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { onSelect(account) }
                )
            }
        }
    }
}

