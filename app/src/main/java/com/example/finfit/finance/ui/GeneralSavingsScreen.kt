package com.example.finfit.finance.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSavingsScreen(
    wallet: AppUserWallet?,
    onSaveWallet: (AppUserWallet) -> Unit,
    onBack: () -> Unit
) {
    if (wallet == null) return

    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustMode by remember { mutableStateOf(true) } // true = Add, false = Subtract/Reset

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quỹ Dự Phòng Chung", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            
            // Icon & Summary
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Savings, null, tint = PrimaryBlue, modifier = Modifier.size(50.dp))
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "Số dư tiết kiệm hiện tại",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            
            Text(
                text = formatCurrency(wallet.generalSavings),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Info Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    "Quỹ dự phòng chung là số tiền bạn trích ra từ tài khoản để dành cho các việc đột xuất. Số tiền này sẽ được trừ khỏi 'Số dư khả dụng' để bạn không lỡ tay tiêu mất.",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
            
            Spacer(Modifier.height(40.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { 
                        adjustMode = true
                        showAdjustDialog = true 
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nạp thêm", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = { 
                        adjustMode = false
                        showAdjustDialog = true 
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Remove, null, tint = Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text("Rút / Sửa", color = Color.Red)
                }
            }
        }
    }

    if (showAdjustDialog) {
        AdjustSavingsDialog(
            currentAmount = wallet.generalSavings,
            isAdding = adjustMode,
            onDismiss = { showAdjustDialog = false },
            onSave = { newAmount ->
                onSaveWallet(wallet.copy(generalSavings = newAmount))
                showAdjustDialog = false
            }
        )
    }
}

@Composable
fun AdjustSavingsDialog(
    currentAmount: Double,
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isRawEdit by remember { mutableStateOf(!isAdding) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAdding) "Nạp thêm quỹ tiết kiệm" else "Điều chỉnh số dư") },
        text = {
            Column {
                if (isAdding) {
                    Text("Nhập số tiền bạn muốn trích thêm vào quỹ dự phòng.", fontSize = 14.sp)
                } else {
                    Text("Nhập số tiền dư mới của quỹ dự phòng (Hoặc nhập 0 để rút hết).", fontSize = 14.sp)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text(if (isAdding) "Số tiền nạp thêm" else "Số dư quỹ mới") },
                    suffix = { Text("đ") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val input = amountText.toDoubleOrNull() ?: 0.0
                    val finalAmount = if (isAdding) currentAmount + input else input
                    onSave(finalAmount)
                }
            ) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
