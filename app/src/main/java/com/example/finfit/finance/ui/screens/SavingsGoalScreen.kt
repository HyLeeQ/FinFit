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
    onBack: () -> Unit,
    onHome: () -> Unit = onBack
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showContributionDialog by remember { mutableStateOf<SavingsGoal?>(null) }
    var lastCompletedGoal by remember { mutableStateOf<SavingsGoal?>(null) }

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
                    if (goals.isEmpty()) {
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
                                Text(
                                    "Bạn có ${goals.size} mục tiêu đang thực hiện",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            
                            itemsIndexed(goals, key = { _, it -> it.id }) { index, goal ->
                                // Staggered entrance for items
                                val itemVisible = remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) { itemVisible.value = true }
                                
                                AnimatedVisibility(
                                    visible = itemVisible.value,
                                    enter = fadeIn(animationSpec = tween(400, delayMillis = index * 100)) + 
                                            slideInHorizontally(initialOffsetX = { 20 }, animationSpec = tween(400, delayMillis = index * 100))
                                ) {
                                    GoalDetailCard(
                                        goal = goal,
                                        onEdit = { selectedGoal = goal },
                                        onContribute = { showContributionDialog = goal }
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
}

@Composable
fun GoalDetailCard(
    goal: SavingsGoal,
    onEdit: () -> Unit,
    onContribute: () -> Unit
) {
    val progress = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val goalColor = Color(goal.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(goal.iconEmoji, fontSize = 28.sp)
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.goalName, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        if (progress >= 1f) "Đã hoàn thành! 🎉" else "Đang thực hiện",
                        color = if (progress >= 1f) goalColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Black, color = goalColor, fontSize = 20.sp)
            }

            Spacer(Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("HIỆN CÓ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    AnimatedAmountText(amount = goal.currentAmount, isHidden = false, color = goalColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("MỤC TIÊU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Text(formatCurrency(goal.targetAmount), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(Modifier.height(16.dp))
            
            // Modern Progress Bar
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(goalColor.copy(alpha = 0.1f))) {
                val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000, easing = FastOutSlowInEasing))
                Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(CircleShape).background(
                    Brush.horizontalGradient(listOf(goalColor.copy(alpha = 0.8f), goalColor))
                ))
            }

            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = onContribute,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (progress >= 1f) Color.Gray.copy(alpha = 0.2f) else goalColor),
                enabled = progress < 1f
            ) {
                Icon(if (progress >= 1f) Icons.Default.CheckCircle else Icons.Default.AddCircle, null)
                Spacer(Modifier.width(8.dp))
                Text(if (progress >= 1f) "Hoàn thành" else "Tiết kiệm thêm", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyGoalsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(160.dp).clip(CircleShape).background(PrimaryBlue.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Savings, null, tint = PrimaryBlue.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text("Bắt đầu thực hiện ước mơ", fontWeight = FontWeight.Black, fontSize = 22.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Hãy chia nhỏ mục tiêu tài chính của bạn và thực hiện chúng từng bước một. FinFit sẽ đồng hành cùng bạn!",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            lineHeight = 22.sp
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
        title = { Text("Tiết kiệm thêm cho mục tiêu", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.iconEmoji, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(goal.goalName, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Nhập số tiền bạn muốn trích từ tài khoản để chuyển vào mục tiêu này.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) amount = it },
                    label = { Text("Số tiền (đ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
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
    uid: String,
    onDismiss: () -> Unit,
    onSave: (SavingsGoal) -> Unit,
    onDelete: (SavingsGoal) -> Unit
) {
    var name by remember { mutableStateOf(goal?.goalName ?: "") }
    var target by remember { mutableStateOf(goal?.targetAmount?.toLong()?.toString() ?: "") }
    var icon by remember { mutableStateOf(goal?.iconEmoji ?: "🎯") }
    var colorHex by remember { mutableLongStateOf(goal?.colorHex ?: 0xFF3B82F6) }

    val colors = listOf(0xFFEF4444, 0xFFF59E0B, 0xFF10B981, 0xFF3B82F6, 0xFF8B5CF6, 0xFFEC4899)
    val emojis = listOf("🎯", "🏠", "🚗", "✈️", "💻", "🎓", "💍", "👶", "🏥", "🎁")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "Mục tiêu mới" else "Chỉnh sửa mục tiêu", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên mục tiêu") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                OutlinedTextField(value = target, onValueChange = { if (it.all { c -> c.isDigit() }) target = it }, label = { Text("Số tiền cần tiết kiệm") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                
                Text("Chọn biểu tượng", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojis.forEach { e ->
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(if (icon == e) Color.Gray.copy(alpha = 0.2f) else Color.Transparent).clickable { icon = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 24.sp) }
                    }
                }

                Text("Màu sắc chủ đề", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    colors.forEach { c ->
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(Color(c)).clickable { colorHex = c }.border(if (colorHex == c) 2.dp else 0.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
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
                        colorHex = colorHex
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
    // Placeholder for a celebration overlay (could be Lottie etc)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("Tuyệt vời!") } },
        title = { Text("🎉 Chúc mừng!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(goal.iconEmoji, fontSize = 60.sp)
                Spacer(Modifier.height(16.dp))
                Text("Bạn đã hoàn thành mục tiêu", textAlign = TextAlign.Center)
                Text(goal.goalName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(goal.colorHex), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("với số tiền ${formatCurrency(goal.targetAmount)}", textAlign = TextAlign.Center)
            }
        },
        shape = RoundedCornerShape(32.dp)
    )
}
