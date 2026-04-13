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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.AccentGreen
import com.example.finfit.ui.theme.PrimaryBlue
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgets: List<FinanceBudget>,
    transactions: List<FinanceTransaction>,
    autoSaveSurplus: Boolean,
    onToggleAutoSave: (Boolean) -> Unit,
    onSaveBudget: (FinanceBudget) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onBack: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<FinanceBudget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hạn mức chi tiêu", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Modern Auto-save Toggle Card
                    AutoSaveCard(autoSaveSurplus, onToggleAutoSave)
                }

                if (budgets.isEmpty()) {
                    item {
                        EmptyBudgetState()
                    }
                } else {
                    item {
                        Text(
                            "HẠN MỨC ĐANG THỰC HIỆN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }
                    
                    itemsIndexed(budgets, key = { _, it -> it.id }) { index, budget ->
                        val itemVisible = remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { itemVisible.value = true }

                        AnimatedVisibility(
                            visible = itemVisible.value,
                            enter = fadeIn(animationSpec = tween(400, delayMillis = index * 100)) + 
                                    slideInHorizontally(initialOffsetX = { 20 }, animationSpec = tween(400, delayMillis = index * 100))
                        ) {
                            BudgetItemCard(
                                budget = budget,
                                spent = calculateSpent(budget, transactions),
                                onEdit = { editingBudget = budget }
                            )
                        }
                    }
                }
                
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    if (showAddDialog || editingBudget != null) {
        AddEditBudgetDialog(
            budget = editingBudget,
            onDismiss = { showAddDialog = false; editingBudget = null },
            onSave = { onSaveBudget(it); showAddDialog = false; editingBudget = null },
            onDelete = { onDeleteBudget(it.id); editingBudget = null }
        )
    }
}

@Composable
fun AutoSaveCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoMode, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tiết kiệm tự động", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    "Theo dõi chi tiêu và trích tiền thừa hàng tuần.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryBlue,
                    uncheckedThumbColor = Color.Gray.copy(alpha = 0.5f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun BudgetItemCard(budget: FinanceBudget, spent: Double, onEdit: () -> Unit) {
    val progress = (spent / budget.amount).coerceIn(0.0, 1.0).toFloat()
    val isNearLimit = progress >= 0.8f
    val accentColor = if (isNearLimit) Color(0xFFEF4444) else PrimaryBlue

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getCategoryIcon(budget.category), null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(budget.category, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("Còn lại ${formatCurrency((budget.amount - spent).coerceAtLeast(0.0))}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (isNearLimit) Color.Red else Color.Gray)
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Progress Bar
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000, easing = FastOutSlowInEasing))
                Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(CircleShape).background(
                    Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.8f), accentColor))
                ))
            }
            
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatCurrency(spent), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Hạn mức ${formatCurrency(budget.amount)}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun EmptyBudgetState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(140.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PieChart, null, modifier = Modifier.size(70.dp), tint = PrimaryBlue.copy(alpha = 0.2f))
        }
        Spacer(Modifier.height(24.dp))
        Text("Kiểm soát chi tiêu", fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(
            "Hãy đặt hạn mức chi tiêu cho từng danh mục để quản lý tài chính tốt hơn.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

fun calculateSpent(budget: FinanceBudget, transactions: List<FinanceTransaction>): Double {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.DAY_OF_MONTH, 1) // Monthly budget
    val monthStart = cal.timeInMillis

    return transactions.filter { 
        it.category == budget.category && 
        it.type == TransactionType.EXPENSE && 
        it.timestamp.toDate().time >= monthStart 
    }.sumOf { it.amount }
}

@Composable
fun AddEditBudgetDialog(
    budget: FinanceBudget?,
    onDismiss: () -> Unit,
    onSave: (FinanceBudget) -> Unit,
    onDelete: (FinanceBudget) -> Unit
) {
    var category by remember { mutableStateOf(budget?.category ?: "Ăn uống") }
    var limitText by remember { mutableStateOf(budget?.amount?.toLong()?.toString() ?: "") }
    var showCategoryPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget == null) "Hạn mức mới" else "Chỉnh sửa hạn mức", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Cài đặt hạn mức chi tiêu hàng tháng cho từng danh mục.", fontSize = 14.sp)
                
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showCategoryPicker = true },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(getCategoryIcon(category), null, tint = PrimaryBlue)
                        Spacer(Modifier.width(16.dp))
                        Text(category, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

                VnAmountTextField(
                    rawValue = limitText,
                    onValueChange = { limitText = it },
                    label = "Hạn mức tháng (đ)",
                    modifier = Modifier.fillMaxWidth(),
                    suffix = ""
                )

                if (budget != null) {
                    TextButton(onClick = { onDelete(budget) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Xóa hạn mức này", color = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                onSave(FinanceBudget(
                        id = budget?.id ?: UUID.randomUUID().toString(),
                        category = category,
                        amount = limitText.toDoubleOrNull() ?: 0.0
                    ))
                },
                enabled = limitText.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        shape = RoundedCornerShape(24.dp)
    )

    if (showCategoryPicker) {
        CategoryPickerDialog(
            onSelected = { category = it; showCategoryPicker = false },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

@Composable
fun CategoryPickerDialog(onSelected: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn danh mục", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(EXPENSE_CATEGORIES) { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelected(cat.label) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(cat.icon, null, tint = cat.color)
                        Spacer(Modifier.width(16.dp))
                        Text(cat.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
        shape = RoundedCornerShape(24.dp)
    )
}
