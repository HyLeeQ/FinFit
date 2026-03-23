package com.example.finfit.finance.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.model.HeldFundItem
import com.example.finfit.ui.theme.PrimaryBlue
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeldFundsManagementScreen(
    wallet: AppUserWallet?,
    onSaveWallet: (AppUserWallet) -> Unit,
    onBack: () -> Unit
) {
    if (wallet == null) return

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<HeldFundItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quỹ nhóm & Tiền giữ hộ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Khoản mới") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Summary Card
            HeldFundSummaryCard(total = wallet.totalHeldFunds)
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                "Danh sách các khoản đang giữ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (wallet.heldFunds.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa có khoản tiền giữ hộ nào.", color = Color.Gray)
                }
            } else {
                wallet.heldFunds.forEach { item ->
                    HeldFundListItem(
                        item = item,
                        onEdit = { editingItem = item },
                        onDelete = {
                            onSaveWallet(wallet.copy(heldFunds = wallet.heldFunds.filter { it.id != item.id }))
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showAddDialog || editingItem != null) {
        AddEditHeldFundDialog(
            item = editingItem,
            onDismiss = { 
                showAddDialog = false
                editingItem = null
            },
            onSave = { newItem ->
                val newList = if (editingItem != null) {
                    wallet.heldFunds.map { if (it.id == editingItem!!.id) newItem else it }
                } else {
                    wallet.heldFunds + newItem
                }
                onSaveWallet(wallet.copy(heldFunds = newList))
                showAddDialog = false
                editingItem = null
            }
        )
    }
}

@Composable
fun HeldFundSummaryCard(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF8B5CF6) // Purple
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "TỔNG TIỀN ĐANG GIỮ",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatCurrency(total),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Đây là số tiền của người khác hoặc các quỹ nhóm mà bạn đang cầm hộ. Tiền này không được tính vào tài sản cá nhân của bạn.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun HeldFundListItem(
    item: HeldFundItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, null, tint = Color(0xFF8B5CF6))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(formatCurrency(item.amount), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun AddEditHeldFundDialog(
    item: HeldFundItem?,
    onDismiss: () -> Unit,
    onSave: (HeldFundItem) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var amount by remember { mutableStateOf(item?.amount?.toLong()?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Thêm khoản giữ hộ" else "Sửa khoản giữ hộ") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên người / Nhóm") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Số tiền") },
                    suffix = { Text("đ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt > 0) {
                        onSave(HeldFundItem(
                            id = item?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            amount = amt
                        ))
                    }
                }
            ) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
