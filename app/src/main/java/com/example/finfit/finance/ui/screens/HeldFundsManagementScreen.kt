package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

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
                containerColor = Color(0xFF8B5CF6), // Purple
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Khoản mới") },
                shape = RoundedCornerShape(20.dp)
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
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(20.dp))
                
                // Summary Card
                HeldFundSummaryCard(total = wallet.totalHeldFunds)
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    "DANH SÁCH CÁC KHOẢN ĐANG GIỮ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )
                
                if (wallet.heldFunds.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inbox, null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Chưa có khoản tiền giữ hộ nào.", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    wallet.heldFunds.forEachIndexed { index, item ->
                        // Staggered Item Animation
                        val itemVisible = remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { itemVisible.value = true }
                        
                        AnimatedVisibility(
                            visible = itemVisible.value,
                            enter = fadeIn(animationSpec = tween(400, delayMillis = index * 100)) + 
                                    slideInHorizontally(initialOffsetX = { 20 }, animationSpec = tween(400, delayMillis = index * 100))
                        ) {
                            Column {
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
                    }
                }
                
                Spacer(Modifier.height(100.dp))
            }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                )
            )
            .padding(28.dp)
    ) {
        Column {
            Text(
                "TỔNG TIỀN ĐANG GIỮ",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(8.dp))
            AnimatedAmountText(
                amount = total,
                isHidden = false,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Tiền của người khác / quỹ nhóm. Không tính vào tài sản cá nhân.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(2.dp))
                Text(formatCurrency(item.amount), color = Color(0xFF8B5CF6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            
            IconButton(
                onClick = { onDelete() },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Red.copy(alpha = 0.05f))
            ) {
                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
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
        title = { 
            Text(
                if (item == null) "Thêm khoản giữ hộ" else "Sửa thông tin",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Nhập tên người hoặc quỹ nhóm và số tiền tương ứng bạn đang cầm giúp.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên người / Nhóm") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                    label = { Text("Số tiền") },
                    suffix = { Text("đ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt > 0) {
                        onSave(HeldFundItem(
                            id = item?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            amount = amt
                        ))
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
