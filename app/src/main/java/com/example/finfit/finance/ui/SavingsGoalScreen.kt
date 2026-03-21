package com.example.finfit.finance.ui

import androidx.compose.animation.*
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
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.*
import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalScreen(
    uid: String,
    goals: List<SavingsGoal>,
    onSaveGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showContributionDialog by remember { mutableStateOf<SavingsGoal?>(null) }
    var lastCompletedGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
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
                text = { Text("Mục tiêu mới") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (goals.isEmpty()) {
                EmptyGoalsState(modifier = Modifier.padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item { 
                        Text(
                            "Bạn có ${goals.size} mục tiêu đang thực hiện",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    items(goals) { goal ->
                        GoalListCard(
                            goal = goal,
                            onEdit = { selectedGoal = goal },
                            onAddMoney = { showContributionDialog = goal }
                        )
                    }
                }
            }

            // Hiển thị ăn mừng nếu vừa hoàn thành mục tiêu
            lastCompletedGoal?.let { goal ->
                GoalCompletionCelebration(
                    goalName = goal.goalName,
                    onDismiss = { lastCompletedGoal = null }
                )
            }
        }

        // Dialogs
        if (showAddDialog || selectedGoal != null) {
            AddEditGoalDialog(
                goal = selectedGoal,
                onDismiss = { 
                    showAddDialog = false
                    selectedGoal = null
                },
                onSave = { updatedGoal ->
                    onSaveGoal(updatedGoal)
                    showAddDialog = false
                    selectedGoal = null
                },
                onDelete = { id ->
                    onDeleteGoal(id)
                    selectedGoal = null
                }
            )
        }

        if (showContributionDialog != null) {
            ContributionDialog(
                goal = showContributionDialog!!,
                onDismiss = { showContributionDialog = null },
                onAdd = { amount ->
                    val oldProgress = (showContributionDialog!!.currentAmount / showContributionDialog!!.targetAmount)
                    val updated = showContributionDialog!!.copy(
                        currentAmount = showContributionDialog!!.currentAmount + amount
                    )
                    onSaveGoal(updated)
                    
                    // Nếu vừa mới cán mốc 100%
                    if (oldProgress < 1.0 && (updated.currentAmount / updated.targetAmount) >= 1.0) {
                        lastCompletedGoal = updated
                    }
                    
                    showContributionDialog = null
                }
            )
        }
    }
}

@Composable
fun GoalListCard(
    goal: SavingsGoal,
    onEdit: () -> Unit,
    onAddMoney: () -> Unit
) {
    val progress = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val isCompleted = progress >= 1f

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(goal.colorHex).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(goal.iconEmoji, fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            goal.goalName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCompleted) {
                            Text(
                                "Đã hoàn thành! 🎊",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            val remaining = goal.targetAmount - goal.currentAmount
                            Text(
                                "Còn thiếu: ${formatCurrency(remaining)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                // Nút nạp thêm
                IconButton(
                    onClick = onAddMoney,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isCompleted) Color.Gray.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    enabled = !isCompleted
                ) {
                    Icon(Icons.Default.PriceCheck, null, tint = if (isCompleted) Color.Gray else MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(goal.colorHex)
                    )
                    Text(
                        "${formatCurrency(goal.currentAmount)} / ${formatCurrency(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Color(goal.colorHex),
                    trackColor = Color(goal.colorHex).copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun EmptyGoalsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(0)) // Placeholder if res not exist, or use internal
        // Ở đây tạm dùng Icon vì chưa có file json lottie thực tế trong raw
        Icon(
            Icons.Default.Savings,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Chưa có mục tiêu nào",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Bắt đầu tiết kiệm cho những ước mơ của bạn ngay!",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGoalDialog(
    goal: SavingsGoal?,
    onDismiss: () -> Unit,
    onSave: (SavingsGoal) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(goal?.goalName ?: "") }
    var target by remember { mutableStateOf(goal?.targetAmount?.toString() ?: "") }
    var icon by remember { mutableStateOf(goal?.iconEmoji ?: "🎯") }
    val colors = listOf(0xFF3B82F6L, 0xFF10B981L, 0xFF8B5CF6L, 0xFFF59E0BL, 0xFFEF4444L)
    var selectedColor by remember { mutableStateOf(goal?.colorHex ?: colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val targetValue = target.toDoubleOrNull() ?: 0.0
                    onSave(
                        goal?.copy(
                            goalName = name,
                            targetAmount = targetValue,
                            iconEmoji = icon,
                            colorHex = selectedColor
                        ) ?: SavingsGoal(
                            goalName = name,
                            targetAmount = targetValue,
                            iconEmoji = icon,
                            colorHex = selectedColor
                        )
                    )
                },
                enabled = name.isNotBlank() && target.toDoubleOrNull() != null
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            if (goal != null) {
                TextButton(onClick = { onDelete(goal.id) }) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Hủy") }
            }
        },
        title = { Text(if (goal == null) "Mục tiêu mới" else "Sửa mục tiêu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên mục tiêu (VD: Mua SH)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Số tiền cần tích lũy") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Icon", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("🎯", "🏍️", "💻", "🏠", "✈️", "💍", "📚", "🎧", "🚗").forEach { emoji ->
                        FilterChip(
                            selected = icon == emoji,
                            onClick = { icon = emoji },
                            label = { Text(emoji) }
                        )
                    }
                }
                
                Text("Màu sắc chủ đề", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    width = 2.dp,
                                    color = if (selectedColor == color) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onAdd: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onAdd(amount.toDoubleOrNull() ?: 0.0) },
                enabled = amount.toDoubleOrNull() != null
            ) {
                Text("Xác nhận nạp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
        title = { Text("Nạp tiền vào: ${goal.goalName}") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Số tiền nạp vào") },
                suffix = { Text("đ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun GoalCompletionCelebration(
    goalName: String,
    onDismiss: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://assets10.lottiefiles.com/packages/lf20_tou9yve4.json"))
    val progress by animateLottieCompositionAsState(composition)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) { Text("Tuyệt vời!") }
        },
        title = { 
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(150.dp)
                )
                Text("Chúc mừng! 🎉", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                "Bạn đã hoàn thành mục tiêu tiết kiệm: $goalName. Bạn thật xuất sắc!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
    
    if (progress >= 1.0f) {
        // onDismiss() // Tự đóng sau khi xong animation? Có thể gây phiền
    }
}

// formatCurrency được sử dụng từ DashboardScreen.kt hoặc file utils chung
// Nếu gặp lỗi conflict, hãy đảm bảo chỉ có 1 bản public trong toàn project.
