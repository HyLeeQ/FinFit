package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.*

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
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.util.*
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalScreen(
    uid: String,
    goals: List<SavingsGoal>,
    wallet: AppUserWallet? = null,
    onSaveGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showContributionDialog by remember { mutableStateOf<SavingsGoal?>(null) }
    var showSimulatorGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var lastCompletedGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    // Sắp xếp goals theo độ ưu tiên (HIGH -> MEDIUM -> LOW) rồi theo tiến độ
    val sortedGoals = remember(goals) {
        goals.sortedWith(compareBy<SavingsGoal> { 
            when(it.priority) {
                GoalPriority.HIGH -> 0
                GoalPriority.MEDIUM -> 1
                GoalPriority.LOW -> 2
            }
        }.thenByDescending { it.currentAmount / it.targetAmount.coerceAtLeast(1.0) })
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Mục tiêu tiết kiệm", fontWeight = FontWeight.Bold) },
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
                    text = { Text("Mục tiêu mới") },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        ) { padding ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(600))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (sortedGoals.isEmpty()) {
                        EmptyGoalsState(modifier = Modifier.padding(padding))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "BẠN CÓ ${sortedGoals.size} MỤC TIÊU ĐANG THỰC HIỆN",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                            
                            itemsIndexed(sortedGoals, key = { _, it -> it.id }) { index, goal ->
                                val itemVisible = remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { itemVisible.value = true }
                                
                                AnimatedVisibility(
                                    visible = itemVisible.value,
                                    enter = fadeIn(animationSpec = tween(400, delayMillis = index * 80)) + 
                                            slideInHorizontally(initialOffsetX = { 20 }, animationSpec = tween(400, delayMillis = index * 80))
                                ) {
                                    GoalDetailCard(
                                        goal = goal,
                                        onEdit = { selectedGoal = goal },
                                        onContribute = { showContributionDialog = goal },
                                        onSimulate = { showSimulatorGoal = goal }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Congratulations Overlay
                    if (lastCompletedGoal != null) {
                        CompletionOverlay(goal = lastCompletedGoal!!, onDismiss = { lastCompletedGoal = null })
                    }
                }
            }
        }
    }

    // Goal Management Dialogs
    if (showAddDialog || selectedGoal != null) {
        AddEditGoalDialog(
            goal = selectedGoal,
            wallet = wallet,
            uid = uid,
            onDismiss = { showAddDialog = false; selectedGoal = null },
            onSave = { onSaveGoal(it); showAddDialog = false; selectedGoal = null },
            onDelete = { onDeleteGoal(it.id); selectedGoal = null }
        )
    }

    if (showContributionDialog != null) {
        ContributionDialog(
            goal = showContributionDialog!!,
            onDismiss = { showContributionDialog = null },
            onConfirm = { amount ->
                val updated = showContributionDialog!!.copy(currentAmount = showContributionDialog!!.currentAmount + amount)
                onSaveGoal(updated)
                if (updated.currentAmount >= updated.targetAmount) {
                    lastCompletedGoal = updated
                }
                showContributionDialog = null
            }
        )
    }

    if (showSimulatorGoal != null) {
        WhatIfSimulationDialog(
            goal = showSimulatorGoal!!,
            onDismiss = { showSimulatorGoal = null }
        )
    }
}

@Composable
fun GoalDetailCard(
    goal: SavingsGoal,
    onEdit: () -> Unit,
    onContribute: () -> Unit,
    onSimulate: () -> Unit
) {
    val progress = (goal.currentAmount / goal.targetAmount.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
    val goalColor = Color(goal.colorHex)

    val strategyLabel = when(goal.strategy) {
        SavingStrategy.FIXED_SCHEDULE -> "Nạp cố định"
        SavingStrategy.PERCENT_OF_INCOME -> "Trích ${goal.strategyValue.toInt()}% thu nhập"
        SavingStrategy.ROUND_UP -> "Làm tròn chi tiêu"
        SavingStrategy.END_OF_MONTH_SURPLUS -> "Dư cuối tháng"
    }

    val priorityLabel = when(goal.priority) {
        GoalPriority.HIGH -> "Ưu tiên Cao"
        GoalPriority.MEDIUM -> "Ưu tiên Vừa"
        GoalPriority.LOW -> "Ưu tiên Thấp"
    }
    val priorityColor = when(goal.priority) {
        GoalPriority.HIGH -> Color(0xFFEF4444)
        GoalPriority.MEDIUM -> Color(0xFFF59E0B)
        GoalPriority.LOW -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(goal.iconEmoji, fontSize = 26.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.goalName, fontWeight = FontWeight.Black, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Strategy badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(goalColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(strategyLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = goalColor)
                        }
                        // Priority badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(priorityColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(priorityLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = priorityColor)
                        }
                    }
                }
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Black, color = goalColor, fontSize = 18.sp)
            }

            Spacer(Modifier.height(18.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("HIỆN CÓ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    AnimatedAmountText(amount = goal.currentAmount, isHidden = false, color = goalColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("MỤC TIÊU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Text(formatCurrency(goal.targetAmount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(Modifier.height(12.dp))
            
            // Progress Bar
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.1f))) {
                val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800, easing = FastOutSlowInEasing))
                Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(CircleShape).background(
                    Brush.horizontalGradient(listOf(goalColor.copy(alpha = 0.8f), goalColor))
                ))
            }

            Spacer(Modifier.height(18.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // What-if simulator button
                OutlinedButton(
                    onClick = onSimulate,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, goalColor.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.QueryStats, null, tint = goalColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mô phỏng", color = goalColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Contribute button
                Button(
                    onClick = onContribute,
                    modifier = Modifier.weight(1.2f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (progress >= 1f) Color.Gray.copy(alpha = 0.2f) else goalColor),
                    enabled = progress < 1f
                ) {
                    Icon(if (progress >= 1f) Icons.Default.CheckCircle else Icons.Default.AddCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (progress >= 1f) "Hoàn thành" else "Tiết kiệm thêm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─── What-if Simulator Dialog ────────────────────────────────────────────────

@Composable
fun WhatIfSimulationDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit
) {
    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    var extraPerWeek by remember { mutableDoubleStateOf(200000.0) } // Default +200k/week

    val baseWeeklySavings = if (goal.autoSavingAmount > 0) goal.autoSavingAmount else 100000.0
    val currentWeeksNeeded = ceil(remaining / baseWeeklySavings).toInt().coerceAtLeast(1)
    val simulatedWeeklySavings = baseWeeklySavings + extraPerWeek
    val newWeeksNeeded = ceil(remaining / simulatedWeeklySavings).toInt().coerceAtLeast(1)
    val weeksSaved = (currentWeeksNeeded - newWeeksNeeded).coerceAtLeast(0)
    val daysSaved = weeksSaved * 7

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔮 Mô phỏng kịch bản (What-if)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Xem mục tiêu của bạn sẽ hoàn thành sớm hơn bao lâu nếu tăng thêm khoản tích lũy hàng tuần.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryBlue.copy(alpha = 0.08f))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Số tiền còn thiếu:", fontSize = 12.sp, color = Color.Gray)
                            Text(formatCurrency(remaining), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Thời gian dự kiến hiện tại:", fontSize = 12.sp, color = Color.Gray)
                            Text("~ $currentWeeksNeeded tuần", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Text("Tăng thêm mỗi tuần:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100000.0, 200000.0, 500000.0, 1000000.0).forEach { amt ->
                        val isSelected = extraPerWeek == amt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { extraPerWeek = amt },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+${(amt/1000).toInt()}k",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Kết quả mô phỏng
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentGreen.copy(alpha = 0.12f))
                        .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "🎉 Dự kiến hoàn thành sau $newWeeksNeeded tuần",
                            fontWeight = FontWeight.Black,
                            color = AccentGreen,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Sớm hơn **$daysSaved ngày** (~$weeksSaved tuần) so với tiến độ ban đầu!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Đóng")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun EmptyGoalsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(140.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Savings, null, tint = PrimaryBlue.copy(alpha = 0.3f), modifier = Modifier.size(70.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Bắt đầu thực hiện ước mơ", fontWeight = FontWeight.Black, fontSize = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Hãy chia nhỏ mục tiêu tài chính của bạn và tích lũy từng bước một.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun ContributionDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nạp thêm cho mục tiêu", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.iconEmoji, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(goal.goalName, fontWeight = FontWeight.Bold)
                }
                VnAmountTextField(
                    rawValue = amount,
                    onValueChange = { amount = it },
                    label = "Số tiền (đ)",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) },
                enabled = amount.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(goal.colorHex))
            ) { Text("Xác nhận") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun AddEditGoalDialog(
    goal: SavingsGoal?,
    wallet: AppUserWallet?,
    uid: String,
    onDismiss: () -> Unit,
    onSave: (SavingsGoal) -> Unit,
    onDelete: (SavingsGoal) -> Unit
) {
    var name by remember { mutableStateOf(goal?.goalName ?: "") }
    var target by remember { mutableStateOf(goal?.targetAmount?.toLong()?.toString() ?: "") }
    var icon by remember { mutableStateOf(goal?.iconEmoji ?: "🎯") }
    var colorHex by remember { mutableLongStateOf(goal?.colorHex ?: 0xFF3B82F6) }
    var strategy by remember { mutableStateOf(goal?.strategy ?: SavingStrategy.FIXED_SCHEDULE) }
    var priority by remember { mutableStateOf(goal?.priority ?: GoalPriority.MEDIUM) }
    var strategyValueText by remember { mutableStateOf(if ((goal?.strategyValue ?: 0.0) > 0) goal!!.strategyValue.toInt().toString() else "10") }
    var linkedHeldFundId by remember { mutableStateOf(goal?.linkedHeldFundId) }

    val colors = listOf(0xFFEF4444, 0xFFF59E0B, 0xFF10B981, 0xFF3B82F6, 0xFF8B5CF6, 0xFFEC4899)
    val emojis = listOf("🎯", "🏠", "🚗", "✈️", "💻", "🎓", "💍", "👶", "🏥", "🎁")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "Mục tiêu mới" else "Chỉnh sửa mục tiêu", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên mục tiêu") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                VnAmountTextField(
                    rawValue = target,
                    onValueChange = { target = it },
                    label = "Số tiền cần tiết kiệm (đ)",
                    modifier = Modifier.fillMaxWidth(),
                    suffix = ""
                )

                // Chọn chiến lược tiết kiệm
                Text("Chiến lược tiết kiệm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SavingStrategy.values().forEach { st ->
                        val isSelected = strategy == st
                        val label = when(st) {
                            SavingStrategy.FIXED_SCHEDULE -> "Nạp cố định định kỳ"
                            SavingStrategy.PERCENT_OF_INCOME -> "Trích % từ mỗi lần nhận thu nhập"
                            SavingStrategy.ROUND_UP -> "Làm tròn số tiền khi chi tiêu (Round-up)"
                            SavingStrategy.END_OF_MONTH_SURPLUS -> "Tích lũy số dư thừa cuối tháng"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { strategy = st }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { strategy = st })
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                if (strategy == SavingStrategy.PERCENT_OF_INCOME) {
                    OutlinedTextField(
                        value = strategyValueText,
                        onValueChange = { strategyValueText = it },
                        label = { Text("Tỷ lệ trích (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Xếp hạng ưu tiên
                Text("Độ ưu tiên mục tiêu", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalPriority.values().forEach { pr ->
                        val isSelected = priority == pr
                        val prLabel = when(pr) { GoalPriority.HIGH -> "Cao 🔥"; GoalPriority.MEDIUM -> "Vừa ⚡"; GoalPriority.LOW -> "Thấp 🍃" }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { priority = pr },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(prLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Chọn biểu tượng
                Text("Chọn biểu tượng", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojis.forEach { e ->
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(if (icon == e) Color.Gray.copy(alpha = 0.2f) else Color.Transparent).clickable { icon = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 22.sp) }
                    }
                }

                // Màu sắc
                Text("Màu chủ đề", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { c ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Color(c)).clickable { colorHex = c }.border(if (colorHex == c) 2.dp else 0.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
                        )
                    }
                }
                
                if (goal != null) {
                    TextButton(onClick = { onDelete(goal) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Xóa mục tiêu này", color = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(SavingsGoal(
                        id = goal?.id ?: UUID.randomUUID().toString(),
                        goalName = name,
                        targetAmount = target.toDoubleOrNull() ?: 0.0,
                        currentAmount = goal?.currentAmount ?: 0.0,
                        iconEmoji = icon,
                        colorHex = colorHex,
                        strategy = strategy,
                        strategyValue = strategyValueText.toDoubleOrNull() ?: 10.0,
                        priority = priority,
                        linkedHeldFundId = linkedHeldFundId
                    ))
                },
                enabled = name.isNotBlank() && target.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun CompletionOverlay(goal: SavingsGoal, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("Tuyệt vời!") } },
        title = { Text("🎉 Chúc mừng!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(goal.iconEmoji, fontSize = 54.sp)
                Spacer(Modifier.height(14.dp))
                Text("Bạn đã hoàn thành mục tiêu", textAlign = TextAlign.Center)
                Text(goal.goalName, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(goal.colorHex), textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("với số tiền ${formatCurrency(goal.targetAmount)}", textAlign = TextAlign.Center)
            }
        },
        shape = RoundedCornerShape(32.dp)
    )
}
