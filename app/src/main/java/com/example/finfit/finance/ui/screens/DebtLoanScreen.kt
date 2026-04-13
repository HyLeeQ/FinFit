package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.AccentGreen
import com.example.finfit.ui.theme.PrimaryBlue
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtLoanScreen(
    items: List<DebtLoan>,
    onSave: (DebtLoan) -> Unit,
    onTogglePaid: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Nợ, 1: Cho vay
    
    val filteredItems = remember(items, selectedTab) {
        val type = if (selectedTab == 0) DebtLoanType.DEBT else DebtLoanType.LOAN
        items.filter { it.type == type }.sortedBy { it.isPaid }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Nợ & Cho vay", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Thêm mới") },
                shape = RoundedCornerShape(20.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Modern Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]).padding(horizontal = 40.dp).clip(CircleShape),
                            height = 3.dp,
                            color = PrimaryBlue
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bạn đang Nợ", fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Medium) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bạn Cho vay", fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Medium) }
                )
            }
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                if (filteredItems.isEmpty()) {
                    EmptyDebtLoanState(selectedTab == 0)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(filteredItems, key = { _, it -> it.id }) { index, item ->
                            val itemVisible = remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { itemVisible.value = true }

                            AnimatedVisibility(
                                visible = itemVisible.value,
                                enter = fadeIn(animationSpec = tween(400, delayMillis = index * 80)) + 
                                        slideInHorizontally(initialOffsetX = { 20 }, animationSpec = tween(400, delayMillis = index * 80))
                            ) {
                                DebtLoanItemCard(
                                    item = item, 
                                    onTogglePaid = { onTogglePaid(item.id, !item.isPaid) }, 
                                    onDelete = { onDelete(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDebtLoanDialog(
            defaultType = if (selectedTab == 0) DebtLoanType.DEBT else DebtLoanType.LOAN,
            onDismiss = { showAddDialog = false },
            onConfirm = { onSave(it); showAddDialog = false }
        )
    }
}

@Composable
fun DebtLoanItemCard(item: DebtLoan, onTogglePaid: () -> Unit, onDelete: () -> Unit) {
    val statusColor = if (item.isPaid) AccentGreen else if (item.type == DebtLoanType.DEBT) Color(0xFFEF4444) else PrimaryBlue
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (item.isPaid) 0.7f else 1f },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.isPaid) Icons.Default.CheckCircle else if (item.type == DebtLoanType.DEBT) Icons.Default.CallReceived else Icons.Default.CallMade,
                    null, tint = statusColor, modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(item.personName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (item.isPaid) Color.Gray else MaterialTheme.colorScheme.onSurface)
                if (item.note.isNotEmpty()) {
                    Text(item.note, fontSize = 13.sp, color = Color.Gray)
                }
                item.dueDate?.let {
                    val isOverdue = it.toDate().before(Date()) && !item.isPaid
                    Text(
                        "Hạn: ${sdf.format(it.toDate())}", 
                        fontSize = 12.sp, 
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverdue) Color.Red else Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(item.amount), fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (item.isPaid) Color.Gray else statusColor)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onTogglePaid,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = statusColor.copy(alpha = 0.05f))
                    ) {
                        Icon(if (item.isPaid) Icons.Default.History else Icons.Default.Check, null, tint = statusColor, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Red.copy(alpha = 0.05f))
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDebtLoanState(isDebt: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(140.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
            Icon(if (isDebt) Icons.Default.BackHand else Icons.Default.VolunteerActivism, null, tint = PrimaryBlue.copy(alpha = 0.2f), modifier = Modifier.size(70.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(if (isDebt) "Bạn không có nợ ai cả" else "Không ai nợ bạn", fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(
            "Mọi thứ đều được kiểm soát tốt! 🎉",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtLoanDialog(defaultType: DebtLoanType, onDismiss: () -> Unit, onConfirm: (DebtLoan) -> Unit) {
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(defaultType) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm ghi chú nợ", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(4.dp)
                ) {
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp)).background(if (type == DebtLoanType.DEBT) Color(0xFFEF4444) else Color.Transparent).clickable { type = DebtLoanType.DEBT }, contentAlignment = Alignment.Center) {
                        Text("Tôi đi vay", color = if (type == DebtLoanType.DEBT) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp)).background(if (type == DebtLoanType.LOAN) PrimaryBlue else Color.Transparent).clickable { type = DebtLoanType.LOAN }, contentAlignment = Alignment.Center) {
                        Text("Cho vay", color = if (type == DebtLoanType.LOAN) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                OutlinedTextField(value = person, onValueChange = { person = it }, label = { Text("Tên người tham gia") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                VnAmountTextField(
                    rawValue = amount,
                    onValueChange = { amount = it },
                    label = "Số tiền (đ)",
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú/Mục đích") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (person.isNotEmpty() && amt > 0) {
                        onConfirm(DebtLoan(id = UUID.randomUUID().toString(), personName = person, amount = amt, type = type, note = note))
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        shape = RoundedCornerShape(24.dp)
    )
}
