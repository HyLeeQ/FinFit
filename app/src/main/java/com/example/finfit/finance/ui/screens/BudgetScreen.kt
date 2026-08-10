package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*
import com.example.finfit.finance.ui.logic.*
import com.example.finfit.core.ui.FinFitTopAppBar

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
import kotlin.math.abs

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
    var showEnvelopeTransferDialog by remember { mutableStateOf(false) }

    // Overall pace calculation (tổng chi tiêu vs tổng hạn mức tháng)
    val totalBudgetObj = remember(budgets) {
        val totalAmt = budgets.sumOf { it.amount + (if (it.isRollover) it.rolloverAmount else 0.0) }
        FinanceBudget(id = "total", category = "Tất cả", amount = totalAmt)
    }
    val overallPace = remember(totalBudgetObj, transactions) {
        BudgetLogic.calculateSpendingPace(totalBudgetObj, transactions)
    }

    Scaffold(
        topBar = {
            FinFitTopAppBar(
                title = "Hạn mức & Phong bì chi tiêu",
                onBack = onBack,
                containerColor = MaterialTheme.colorScheme.background,
                actions = {
                    if (budgets.size >= 2) {
                        IconButton(onClick = { showEnvelopeTransferDialog = true }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Chuyển tiền phong bì", tint = PrimaryBlue)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Hạn mức mới") },
                shape = RoundedCornerShape(20.dp)
            )
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
                    // Spending Pace & Forecast Card
                    if (budgets.isNotEmpty() && totalBudgetObj.amount > 0) {
                        SpendingPaceForecastCard(overallPace)
                    }
                }

                item {
                    // Modern Auto-save Toggle Card
                    AutoSaveCard(autoSaveSurplus, onToggleAutoSave)
                }

                if (budgets.isEmpty()) {
                    item {
                        EmptyBudgetState { showAddDialog = true }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "DANH SÁCH HẠN MỨC (${budgets.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                            )
                            if (budgets.size >= 2) {
                                TextButton(onClick = { showEnvelopeTransferDialog = true }) {
                                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Chuyển quỹ phong bì", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    
                    itemsIndexed(budgets, key = { _, it -> it.id }) { index, budget ->
                        val itemVisible = remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { itemVisible.value = true }

                        val pace = remember(budget, transactions) {
                            BudgetLogic.calculateSpendingPace(budget, transactions)
                        }

                        AnimatedVisibility(
                            visible = itemVisible.value,
                            enter = fadeIn(animationSpec = tween(400, delayMillis = index * 80)) + 
                                    slideInHorizontally(initialOffsetX = { 20 }, animationSpec = tween(400, delayMillis = index * 80))
                        ) {
                            BudgetItemCard(
                                budget = budget,
                                pace = pace,
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
            transactions = transactions,
            onDismiss = { showAddDialog = false; editingBudget = null },
            onSave = { onSaveBudget(it); showAddDialog = false; editingBudget = null },
            onDelete = { onDeleteBudget(it.id); editingBudget = null }
        )
    }

    if (showEnvelopeTransferDialog) {
        EnvelopeTransferDialog(
            budgets = budgets,
            onDismiss = { showEnvelopeTransferDialog = false },
            onTransfer = { fromBudget, toBudget, transferAmount ->
                val updatedFrom = fromBudget.copy(amount = (fromBudget.amount - transferAmount).coerceAtLeast(0.0))
                val updatedTo = toBudget.copy(amount = toBudget.amount + transferAmount)
                onSaveBudget(updatedFrom)
                onSaveBudget(updatedTo)
                showEnvelopeTransferDialog = false
            }
        )
    }
}

// ─── Pace & Forecast Card ───────────────────────────────────────────────────

@Composable
fun SpendingPaceForecastCard(pace: BudgetPaceResult) {
    val isOver = pace.isProjectedToOverspend
    val badgeColor = if (isOver) Color(0xFFEF4444) else AccentGreen

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡ Tốc độ & Dự báo chi tiêu", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isOver) "Có nguy cơ vượt" else "Đang an toàn",
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // AI Forecast Summary
            Text(
                pace.paceSummary,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(16.dp))

            // Dual Progress bar: Days elapsed vs Spent ratio
            val spentRatio = (pace.spentSoFar / pace.totalBudget).coerceIn(0.0, 1.5).toFloat()
            val timeRatio = (pace.daysElapsed.toFloat() / pace.totalDaysInMonth.toFloat()).coerceIn(0.0f, 1.0f)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Đã qua ${pace.daysElapsed}/${pace.totalDaysInMonth} ngày (${(timeRatio * 100).toInt()}%)", fontSize = 11.sp, color = Color.Gray)
                    Text("Đã chi ${(spentRatio * 100).toInt()}% hạn mức", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    // Marker for time elapsed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(timeRatio)
                            .fillMaxHeight()
                            .background(Color.Gray.copy(alpha = 0.25f))
                    )
                    // Actual spent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((spentRatio).coerceAtMost(1.0f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                }
            }
        }
    }
}

@Composable
fun AutoSaveCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoMode, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tự động trích tiền thừa cuối tuần", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Tự động chuyển phần dư hạn mức sang quỹ tiết kiệm.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryBlue
                )
            )
        }
    }
}

@Composable
fun BudgetItemCard(
    budget: FinanceBudget,
    pace: BudgetPaceResult,
    onEdit: () -> Unit
) {
    val totalEffective = budget.amount + (if (budget.isRollover) budget.rolloverAmount else 0.0)
    val progress = (pace.spentSoFar / totalEffective.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
    val isNearLimit = progress >= 0.85f
    val accentColor = if (isNearLimit) Color(0xFFEF4444) else PrimaryBlue

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getCategoryIcon(budget.category), null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(budget.category, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        if (budget.isEnvelope) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Phong bì", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                            }
                        }
                    }
                    Text("Còn lại ${formatCurrency((totalEffective - pace.spentSoFar).coerceAtLeast(0.0))}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (isNearLimit) Color.Red else Color.Gray)
                }
            }
            
            // Rollover badge nếu có cộng dồn từ tháng trước
            if (budget.isRollover && budget.rolloverAmount > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGreen.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Savings, null, tint = AccentGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Đã cộng dồn +${formatCurrency(budget.rolloverAmount)} từ tháng trước",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            
            // Progress Bar
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800, easing = FastOutSlowInEasing))
                Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(CircleShape).background(
                    Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.8f), accentColor))
                ))
            }
            
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Đã chi: ${formatCurrency(pace.spentSoFar)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Hạn mức: ${formatCurrency(totalEffective)}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun EmptyBudgetState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PieChart, null, modifier = Modifier.size(60.dp), tint = PrimaryBlue.copy(alpha = 0.3f))
        }
        Spacer(Modifier.height(20.dp))
        Text("Chưa có hạn mức nào", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(
            "Hãy đặt hạn mức chi tiêu cho từng danh mục hoặc thiết lập phong bì chi tiêu để rèn luyện kỷ luật tài chính.",
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAdd, shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Tạo hạn mức đầu tiên")
        }
    }
}

@Composable
fun AddEditBudgetDialog(
    budget: FinanceBudget?,
    transactions: List<FinanceTransaction>,
    onDismiss: () -> Unit,
    onSave: (FinanceBudget) -> Unit,
    onDelete: (FinanceBudget) -> Unit
) {
    var category by remember { mutableStateOf(budget?.category ?: "Ăn uống") }
    var limitText by remember { mutableStateOf(budget?.amount?.toLong()?.toString() ?: "") }
    var isRollover by remember { mutableStateOf(budget?.isRollover ?: false) }
    var isEnvelope by remember { mutableStateOf(budget?.isEnvelope ?: false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget == null) "Hạn mức mới" else "Chỉnh sửa hạn mức", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Chọn danh mục
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showCategoryPicker = true },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(getCategoryIcon(category), null, tint = PrimaryBlue)
                        Spacer(Modifier.width(14.dp))
                        Text(category, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

                // AI Suggestion Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryBlue.copy(alpha = 0.08f))
                        .clickable {
                            val suggested = BudgetLogic.suggestBudgetAmount(category, transactions)
                            limitText = suggested.toLong().toString()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("Gợi ý từ lịch sử 3 tháng", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                    Text("Áp dụng", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Black)
                }

                VnAmountTextField(
                    rawValue = limitText,
                    onValueChange = { limitText = it },
                    label = "Hạn mức tháng (đ)",
                    modifier = Modifier.fillMaxWidth(),
                    suffix = ""
                )

                // Rollover Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cộng dồn số dư (Rollover)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Chuyển tiền dư tháng này sang tháng sau", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = isRollover, onCheckedChange = { isRollover = it })
                }

                // Envelope Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chế độ Phong bì cứng (Envelope)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Kỷ luật chặt chẽ, không chi quá hạn", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = isEnvelope, onCheckedChange = { isEnvelope = it })
                }

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
                        amount = limitText.toDoubleOrNull() ?: 0.0,
                        isRollover = isRollover,
                        rolloverAmount = budget?.rolloverAmount ?: 0.0,
                        isEnvelope = isEnvelope,
                        envelopeAllocated = budget?.envelopeAllocated ?: 0.0
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

// ─── Dialog Chuyển Tiền Giữa Các Phong Bì (Envelope Transfer) ────────────────

@Composable
fun EnvelopeTransferDialog(
    budgets: List<FinanceBudget>,
    onDismiss: () -> Unit,
    onTransfer: (fromBudget: FinanceBudget, toBudget: FinanceBudget, amount: Double) -> Unit
) {
    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember { mutableIntStateOf(if (budgets.size > 1) 1 else 0) }
    var amountText by remember { mutableStateOf("") }

    val fromBudget = budgets.getOrNull(fromIndex)
    val toBudget = budgets.getOrNull(toIndex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chuyển quỹ giữa các phong bì", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Khi một phong bì chi tiêu hết hạn mức, bạn có thể chuyển bớt hạn mức từ phong bì khác sang.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Từ phong bì
                Text("Chuyển từ:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(fromBudget?.category ?: "", fontWeight = FontWeight.Bold)
                    Text("Hạn mức: ${formatCurrency(fromBudget?.amount ?: 0.0)}", fontSize = 11.sp, color = PrimaryBlue)
                }

                // Sang phong bì
                Text("Chuyển sang:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(toBudget?.category ?: "", fontWeight = FontWeight.Bold)
                    Text("Hạn mức: ${formatCurrency(toBudget?.amount ?: 0.0)}", fontSize = 11.sp, color = AccentGreen)
                }

                VnAmountTextField(
                    rawValue = amountText,
                    onValueChange = { amountText = it },
                    label = "Số tiền chuyển (đ)",
                    modifier = Modifier.fillMaxWidth(),
                    suffix = ""
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (fromBudget != null && toBudget != null && amt > 0) {
                        onTransfer(fromBudget, toBudget, amt)
                    }
                },
                enabled = amountText.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                shape = RoundedCornerShape(12.dp)
            ) { Text("Chuyển tiền") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        shape = RoundedCornerShape(24.dp)
    )
}
