package com.example.finfit.finance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Nợ, 1: Cho vay
    
    val filteredItems = remember(items, selectedTab) {
        val type = if (selectedTab == 0) DebtLoanType.DEBT else DebtLoanType.LOAN
        items.filter { it.type == type }.sortedBy { it.isPaid }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nợ & Cho vay", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = PrimaryBlue, contentColor = Color.White) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Bạn đang Nợ") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Bạn Cho vay") })
            }
            
            if (filteredItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có dữ liệu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems) { item ->
                        DebtLoanItemCard(item, onTogglePaid = { onTogglePaid(item.id, !item.isPaid) }, onDelete = { onDelete(item.id) })
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(
                    if (item.isPaid) Icons.Default.CheckCircle else if (item.type == DebtLoanType.DEBT) Icons.Default.CallReceived else Icons.Default.CallMade,
                    null, tint = statusColor, modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(item.personName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (item.note.isNotEmpty()) {
                    Text(item.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item.dueDate?.let {
                    Text("Hạn: ${sdf.format(it.toDate())}", fontSize = 11.sp, color = if (it.toDate().before(Date()) && !item.isPaid) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(item.amount), fontWeight = FontWeight.Black, color = if (item.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                Row {
                    IconButton(onClick = onTogglePaid) {
                        Icon(if (item.isPaid) Icons.Default.Undo else Icons.Default.Done, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
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
        title = { Text("Thêm mới", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = type == DebtLoanType.DEBT, onClick = { type = DebtLoanType.DEBT }, label = { Text("Đi vay") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = type == DebtLoanType.LOAN, onClick = { type = DebtLoanType.LOAN }, label = { Text("Cho vay") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = person, onValueChange = { person = it }, label = { Text("Người nợ/cho vay") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { if (it.all { it.isDigit() }) amount = it }, label = { Text("Số tiền") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (person.isNotEmpty() && amt > 0) {
                    onConfirm(DebtLoan(personName = person, amount = amt, type = type, note = note))
                }
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
