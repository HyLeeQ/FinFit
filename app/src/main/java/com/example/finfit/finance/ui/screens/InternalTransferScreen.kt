package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.AppBankAccount
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalTransferScreen(
    wallet: AppUserWallet,
    onNavigateBack: () -> Unit,
    onConfirmTransfer: (fromAccountId: String, toAccountId: String, amount: Double, note: String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("Chuyển tiền nội bộ") }
    
    val accounts = wallet.accounts
    var fromAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var toAccount by remember { mutableStateOf(accounts.getOrNull(1) ?: accounts.firstOrNull()) }
    
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chuyển khoản nội bộ", fontWeight = FontWeight.Black) },
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
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                
                // Visual Flow of Transfer
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TransferAccountCard(
                            label = "NGUỒN TIỀN",
                            account = fromAccount,
                            onClick = { showFromPicker = true },
                            accentColor = PrimaryBlue
                        )
                    }
                    
                    Column(
                        modifier = Modifier.width(60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows, 
                                null, 
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        TransferAccountCard(
                            label = "ĐÍCH ĐẾN",
                            account = toAccount,
                            onClick = { showToPicker = true },
                            accentColor = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                // Amount Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SỐ TIỀN CHUYỂN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = PrimaryBlue, letterSpacing = 1.sp)
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("đ", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Spacer(Modifier.width(16.dp))
                            BasicTextField(
                                value = amountText,
                                onValueChange = { if (it.all { c -> c.isDigit() }) amountText = it },
                                textStyle = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, color = PrimaryBlue),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = SolidColor(PrimaryBlue)
                            )
                        }
                        if (amountText.isNotEmpty()) {
                            val amtValue = amountText.toDoubleOrNull() ?: 0.0
                            Text(
                                "Từ dư: ${formatCurrency(fromAccount?.amount ?: 0.0)}",
                                fontSize = 12.sp,
                                color = if (amtValue > (fromAccount?.amount ?: 0.0)) Color.Red else Color.Gray,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú chuyển khoản") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                )

                Spacer(Modifier.height(48.dp))

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (fromAccount != null && toAccount != null && amt > 0) {
                            onConfirmTransfer(fromAccount!!.id, toAccount!!.id, amt, note)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = amountText.isNotEmpty() && fromAccount?.id != toAccount?.id
                ) {
                    Text("Xác nhận chuyển tiền", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
                
                Spacer(Modifier.height(100.dp))
            }
        }
    }

    if (showFromPicker) {
        AccountPickerDialog(
            accounts = accounts,
            onSelected = { fromAccount = it; showFromPicker = false },
            onDismiss = { showFromPicker = false }
        )
    }
    if (showToPicker) {
        AccountPickerDialog(
            accounts = accounts,
            onSelected = { toAccount = it; showToPicker = false },
            onDismiss = { showToPicker = false }
        )
    }
}

@Composable
fun TransferAccountCard(label: String, account: AppBankAccount?, onClick: () -> Unit, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxSize().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp), 
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                account?.name ?: "Chọn thẻ", 
                fontWeight = FontWeight.Bold, 
                fontSize = 13.sp, 
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}
