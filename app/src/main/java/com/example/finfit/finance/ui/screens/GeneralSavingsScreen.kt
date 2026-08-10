package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.model.AppUserWallet
import com.example.finfit.finance.model.SavingsGoal
import com.example.finfit.ui.theme.AccentGreen
import com.example.finfit.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSavingsScreen(
    wallet: AppUserWallet?,
    goals: List<SavingsGoal>,             // Mục tiêu cá nhân để trích tiền vào
    onSaveWallet: (AppUserWallet) -> Unit,
    onSaveGoal: (SavingsGoal) -> Unit,    // Lưu mục tiêu sau khi trích tiền
    onBack: () -> Unit
) {
    if (wallet == null) return

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var showAdjustDialog  by remember { mutableStateOf(false) }
    var showTransferToGoal by remember { mutableStateOf(false) } // Trích sang mục tiêu cá nhân

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quỹ Dự Phòng Chung", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                // ── Hero card ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlue.copy(alpha = 0.75f)))
                        )
                        .padding(28.dp),
                ) {
                    Column {
                        Text(
                            "SỐ DƯ TIẾT KIỆM CHUNG",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        AnimatedAmountText(
                            amount = wallet.generalSavings,
                            isHidden = false,
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(14.dp))
                        // Tiến trình tổng góp từ mục tiêu cá nhân
                        val totalGoalTarget = goals.sumOf { it.targetAmount }.coerceAtLeast(1.0)
                        val totalGoalCurrent = goals.sumOf { it.currentAmount }
                        val progress = (totalGoalCurrent / totalGoalTarget).coerceIn(0.0, 1.0).toFloat()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bao gồm ${goals.size} mục tiêu cá nhân", color = Color.White.copy(0.7f), fontSize = 11.sp)
                            Text("${(progress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.fillMaxWidth().height(6.dp)
                                .clip(CircleShape).background(Color.White.copy(0.2f))
                        ) {
                            val animProg by animateFloatAsState(progress, tween(1000, easing = FastOutSlowInEasing), label = "")
                            Box(
                                Modifier.fillMaxWidth(animProg).fillMaxHeight()
                                    .clip(CircleShape).background(Color.White.copy(0.9f))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))



                // ── Danh sách mục tiêu liên kết ─────────────────────────────
                if (goals.isNotEmpty()) {
                    Text(
                        "MỤC TIÊU TIẾT KIỆM CÁ NHÂN",
                        fontSize = 11.sp, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        goals.forEach { goal ->
                            LinkedGoalRow(goal = goal)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── Info card ───────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Khi chi tiêu vượt hạn mức, FinFit tự động khấu trừ từ quỹ này. Bạn cũng có thể trích một phần sang mục tiêu tiết kiệm cá nhân.",
                            fontSize = 12.sp, lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Action Buttons ──────────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showAdjustDialog = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Chỉnh sửa quỹ", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Trích sang mục tiêu cá nhân
                if (goals.isNotEmpty()) {
                    Button(
                        onClick = { showTransferToGoal = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Trích sang Mục tiêu cá nhân", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(60.dp))
            }
        }

        // Dialogs
        if (showAdjustDialog) {
            AdjustSavingsDialog(
                currentAmount = wallet.generalSavings,
                onDismiss = { showAdjustDialog = false },
                onConfirm = { amount ->
                    onSaveWallet(wallet.copy(generalSavings = amount.coerceAtLeast(0.0)))
                    showAdjustDialog = false
                }
            )
        }

        if (showTransferToGoal) {
            TransferToGoalDialog(
                generalSavings = wallet.generalSavings,
                goals = goals,
                onDismiss = { showTransferToGoal = false },
                onConfirm = { goal, amount ->
                    // Trừ khỏi quỹ chung, cộng vào mục tiêu cá nhân
                    val newGeneral = (wallet.generalSavings - amount).coerceAtLeast(0.0)
                    onSaveWallet(wallet.copy(generalSavings = newGeneral))
                    onSaveGoal(goal.copy(currentAmount = goal.currentAmount + amount))
                    showTransferToGoal = false
                }
            )
        }


    }
}

// ─── Hiển thị 1 mục tiêu liên kết ────────────────────────────────────────────
@Composable
fun LinkedGoalRow(goal: SavingsGoal) {
    val progress = (goal.currentAmount / goal.targetAmount.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
    val goalColor = Color(goal.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(goal.iconEmoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(goal.goalName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(goalColor.copy(0.12f))) {
                    val animProg by animateFloatAsState(progress, tween(800, easing = FastOutSlowInEasing), label = "")
                    Box(Modifier.fillMaxWidth(animProg).fillMaxHeight().clip(CircleShape).background(goalColor))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(goal.currentAmount), color = goalColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("/ ${formatCurrency(goal.targetAmount)}", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

// ─── Dialog trích tiền sang mục tiêu cá nhân ─────────────────────────────────
@Composable
fun TransferToGoalDialog(
    generalSavings: Double,
    goals: List<SavingsGoal>,
    onDismiss: () -> Unit,
    onConfirm: (SavingsGoal, Double) -> Unit
) {
    var selectedGoal by remember { mutableStateOf(goals.first()) }
    var amountText   by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trích sang Mục tiêu cá nhân", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Số dư quỹ chung hiện có: ${formatCurrency(generalSavings)}",
                    fontSize = 13.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold
                )

                // Chọn mục tiêu
                Text("Chọn mục tiêu nhận tiền", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(goals) { goal ->
                        val isSelected = goal.id == selectedGoal.id
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedGoal = goal },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(goal.colorHex).copy(0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, Color(goal.colorHex)) else null
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(goal.iconEmoji, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(goal.goalName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                                    Text("Còn thiếu ${formatCurrency(remaining)}", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = Color(goal.colorHex), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                VnAmountTextField(
                    rawValue = amountText,
                    onValueChange = { amountText = it },
                    label = "Số tiền trích (đ)",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val amount = amountText.toDoubleOrNull() ?: 0.0
                if (amount > generalSavings) {
                    Text("⚠️ Vượt quá số dư quỹ chung", color = Color.Red, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onConfirm(selectedGoal, amount) },
                enabled = amount > 0 && amount <= generalSavings,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) { Text("Trích tiền") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        shape = RoundedCornerShape(24.dp)
    )
}

// ─── Dialog nạp / rút quỹ chung ──────────────────────────────────────────────
@Composable
fun AdjustSavingsDialog(
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(currentAmount.toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa Quỹ Dự Phòng", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Nhập số dư chính xác của quỹ dự phòng hiện tại.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VnAmountTextField(
                    rawValue = amountText,
                    onValueChange = { amountText = it },
                    label = "Số dư hiện tại (đ)",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountText.toDoubleOrNull() ?: 0.0) },
                enabled = amountText.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cập nhật") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
        shape = RoundedCornerShape(24.dp)
    )
}

