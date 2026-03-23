package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

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
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(30.dp))
                
                // Icon & Summary with pulsing scale effect
                val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseScale"
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(PrimaryBlue.copy(alpha = 0.12f), RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Savings, null, tint = PrimaryBlue, modifier = Modifier.size(60.dp))
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    "SỐ DƯ TIẾT KIỆM",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                AnimatedAmountText(
                    amount = wallet.generalSavings,
                    isHidden = false,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black
                )
                
                Spacer(Modifier.height(48.dp))
                
                // Info Row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Quỹ dự phòng chung là số tiền bạn trích ra từ tài khoản để dành cho các việc đột xuất. Số tiền này sẽ được trừ khỏi 'Số dư khả dụng' để bạn không lỡ tay tiêu mất.",
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(Modifier.height(48.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { adjustMode = true; showAdjustDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tiết kiệm thêm", fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = { adjustMode = false; showAdjustDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Remove, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Rút tiền", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(50.dp))
            }
        }

        if (showAdjustDialog) {
            AdjustSavingsDialog(
                mode = adjustMode,
                currentAmount = wallet.generalSavings,
                onDismiss = { showAdjustDialog = false },
                onConfirm = { amount ->
                    val updatedAmount = if (adjustMode) {
                        wallet.generalSavings + amount
                    } else {
                        (wallet.generalSavings - amount).coerceAtLeast(0.0)
                    }
                    onSaveWallet(wallet.copy(generalSavings = updatedAmount))
                    showAdjustDialog = false
                }
            )
        }
    }
}

@Composable
fun AdjustSavingsDialog(
    mode: Boolean,
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode) "Tiết kiệm thêm" else "Rút tiền dự phòng") },
        text = {
            Column {
                Text(
                    if (mode) "Nhập số tiền bạn muốn trích thêm vào quỹ dự phòng." 
                    else "Nhập số tiền bạn muốn rút từ quỹ về số dư khả dụng.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("Số tiền (đ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                if (!mode) {
                    Text(
                        "Tối đa: ${formatCurrency(currentAmount)}",
                        fontSize = 12.sp,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val value = amountText.toDoubleOrNull() ?: 0.0
                    onConfirm(value)
                },
                enabled = amountText.isNotBlank() && amountText.toDoubleOrNull() != null
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
