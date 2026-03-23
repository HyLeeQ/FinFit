package com.example.finfit.finance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgets: List<FinanceBudget>,
    transactions: List<FinanceTransaction>,
    onSaveBudget: (FinanceBudget) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hạn mức chi tiêu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        if (budgets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PieChart, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("Bạn chưa đặt hạn mức nào", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(budgets) { budget ->
                    BudgetItemCard(
                        budget = budget,
                        spent = calculateSpentForBudget(budget, transactions),
                        onDelete = { onDeleteBudget(budget.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { budget -> 
                onSaveBudget(budget)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun BudgetItemCard(budget: FinanceBudget, spent: Double, onDelete: () -> Unit) {
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
    val isOverBudget = spent > budget.amount
    val progressColor = if (isOverBudget) Color(0xFFEF4444) else if (progress > 0.8f) Color(0xFFF59E0B) else AccentGreen

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(budget.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (budget.period == BudgetPeriod.WEEKLY) "Hạn mức tuần" else "Hạn mức tháng",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${formatCurrency(spent)} đã dùng",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Còn ${formatCurrency((budget.amount - spent).coerceAtLeast(0.0))}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.1f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Tổng hạn mức: ${formatCurrency(budget.amount)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(onDismiss: () -> Unit, onConfirm: (FinanceBudget) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var showCategoryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đặt hạn mức mới", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { it.isDigit() }) amount = it },
                    label = { Text("Số tiền hạn mức") },
                    suffix = { Text("đ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = period == BudgetPeriod.WEEKLY,
                        onClick = { period = BudgetPeriod.WEEKLY },
                        label = { Text("Tuần") }
                    )
                    FilterChip(
                        selected = period == BudgetPeriod.MONTHLY,
                        onClick = { period = BudgetPeriod.MONTHLY },
                        label = { Text("Tháng") }
                    )
                }

                Box {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().clickable { showCategoryMenu = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(selectedCategory)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        DropdownMenuItem(text = { Text("Tất cả") }, onClick = { selectedCategory = "Tất cả"; showCategoryMenu = false })
                        EXPENSE_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.label) }, onClick = { selectedCategory = cat.label; showCategoryMenu = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(FinanceBudget(amount = amt, period = period, category = selectedCategory))
                    }
                },
                enabled = amount.isNotEmpty()
            ) { Text("Xác nhận") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

fun calculateSpentForBudget(budget: FinanceBudget, transactions: List<FinanceTransaction>): Double {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance()
    
    return transactions.filter { tx ->
        if (tx.type != TransactionType.EXPENSE) return@filter false
        if (budget.category != "Tất cả" && tx.category != budget.category) return@filter false
        
        cal.time = tx.timestamp.toDate()
        
        when (budget.period) {
            BudgetPeriod.WEEKLY -> {
                now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == cal.get(Calendar.WEEK_OF_YEAR)
            }
            BudgetPeriod.MONTHLY -> {
                now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
            }
        }
    }.sumOf { it.amount }
}
