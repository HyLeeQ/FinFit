package com.example.finfit.finance.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.core.model.InsightPriority
import com.example.finfit.core.navigation.Routes
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.*
import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.logic.*

@Composable
fun PlanProgressSection(
    transactions: List<FinanceTransaction>,
    budgets: List<FinanceBudget>,
    onNavigate: (String) -> Unit
) {
    val now = remember { java.util.Calendar.getInstance() }
    val currentMonth = now.get(java.util.Calendar.MONTH)
    val currentYear = now.get(java.util.Calendar.YEAR)
    
    val currentMonthExpenditure = remember(transactions) {
        val tempCal = java.util.Calendar.getInstance()
        transactions.filter { tx ->
            if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.GROUP_PREPAYMENT) {
                tempCal.time = tx.timestamp.toDate()
                tempCal.get(java.util.Calendar.MONTH) == currentMonth && tempCal.get(java.util.Calendar.YEAR) == currentYear
            } else false
        }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }
    }
    
    val currentTotalBudget = remember(budgets) {
        budgets.filter { it.period == BudgetPeriod.MONTHLY && it.category == "Tất cả" }.sumOf { it.amount }
    }

    if (currentTotalBudget > 0) {
        val progress = (currentMonthExpenditure / currentTotalBudget).coerceIn(0.0, 1.0).toFloat()
        val remaining = (currentTotalBudget - currentMonthExpenditure).coerceAtLeast(0.0)
        
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tiến độ kế hoạch", fontWeight = FontWeight.Black, fontSize = 20.sp)
                TextButton(onClick = { onNavigate(Routes.BUDGET) }) {
                    Text("Chi tiết", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .clickable { onNavigate(Routes.BUDGET) }
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hạn mức tháng này", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            Text(formatCurrency(currentTotalBudget), color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (progress > 0.9f) Color.Red.copy(alpha = 0.2f) else Color.Green.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (progress > 0.9f) "Sắp vượt hạn mức" else "Đang kiểm soát tốt",
                                color = if (progress > 0.9f) Color(0xFFFF4D4D) else Color(0xFF4ADE80),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = if (progress > 0.9f) Color(0xFFFF4D4D) else Color(0xFF3B82F6),
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Đã chi: ${formatCurrency(currentMonthExpenditure)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            "Còn lại: ${formatCurrency(remaining)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsGoalsSection(goals: List<SavingsGoal>, onNavigate: (String) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mục tiêu tiết kiệm", fontWeight = FontWeight.Black, fontSize = 18.sp)
            TextButton(onClick = { onNavigate(Routes.SAVINGS_GOALS) }) {
                Text("Xem tất cả →", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { onNavigate(Routes.SAVINGS_GOALS) }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Chưa có mục tiêu nào", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 14.sp)
                    Text("Nhấn để tạo mục tiêu đầu tiên", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                goals.forEach { goal ->
                    val progress = (goal.currentAmount / goal.targetAmount.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
                    val goalColor = Color(goal.colorHex)
                    val bgBrush = remember(goal.colorHex) {
                        Brush.linearGradient(
                            listOf(goalColor.copy(alpha = 0.15f), goalColor.copy(alpha = 0.05f))
                        )
                    }
                    val animatedProgress = remember { Animatable(0f) }
                    LaunchedEffect(progress) {
                        animatedProgress.animateTo(
                            targetValue = progress,
                            animationSpec = tween(800, easing = FastOutSlowInEasing)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(bgBrush)
                            .border(1.dp, goalColor.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .clickable { onNavigate(Routes.SAVINGS_GOALS) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(goal.iconEmoji, fontSize = 22.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = goalColor
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                goal.goalName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(goalColor.copy(alpha = 0.12f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress.value)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(goalColor)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                formatCurrency(goal.currentAmount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "/ ${formatCurrency(goal.targetAmount)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<FinanceTransaction>,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onNavigate: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Giao dịch gần đây", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            TextButton(onClick = { onNavigate(Routes.TRANSACTION_HISTORY) }) {
                Text("Xem tất cả", color = PrimaryBlue, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (transactions.isEmpty()) {
            Text("Chưa có giao dịch nào", color = Color.Gray, fontStyle = FontStyle.Italic)
        } else {
            transactions.take(5).forEach { transaction ->
                TransactionListItem(transaction) { onEditTransaction(transaction) }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun InsightBannerSection(
    insights: List<com.example.finfit.core.model.HealthFinanceInsight>,
    onInsightClick: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ĐẤI SÓNG & TÀI CHÍNH",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            insights.forEach { insight ->
                InsightCard(insight = insight, onClick = { onInsightClick(insight.actionRoute) })
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: com.example.finfit.core.model.HealthFinanceInsight,
    onClick: () -> Unit
) {
    val priorityColor = when (insight.priority) {
        InsightPriority.HIGH   -> Color(0xFFEF4444)
        InsightPriority.MEDIUM -> PrimaryBlue
        InsightPriority.LOW    -> Color(0xFF6B7280)
    }
    val bgColor = priorityColor.copy(alpha = 0.06f)
    val borderColor = priorityColor.copy(alpha = 0.15f)

    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(insight.emoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    insight.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                insight.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (insight.financialImpact != 0.0) {
                Spacer(Modifier.height(8.dp))
                val impactStr = if (insight.financialImpact > 0) "↗ +${formatCurrency(insight.financialImpact)}" else "↘ ${formatCurrency(insight.financialImpact)}"
                val impactColor = if (insight.financialImpact > 0) Color(0xFF10B981) else Color(0xFFEF4444)
                Text(impactStr, fontSize = 11.sp, color = impactColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AccountCard(account: AppBankAccount, onToggle: () -> Unit) {
    val bankInfo = remember(account.bankCode) { SUPPORTED_BANKS.find { it.code == account.bankCode } ?: SUPPORTED_BANKS.last() }
    val gradientBrush = remember(account.colorIndex, bankInfo.primaryColorHex) {
        Brush.linearGradient(cardGradient(account.colorIndex, bankInfo.primaryColorHex))
    }

    Box(modifier = Modifier.width(300.dp).height(170.dp).clip(RoundedCornerShape(24.dp)).background(gradientBrush).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bankInfo.emoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(bankInfo.displayName.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text(account.purpose.iconEmoji, fontSize = 14.sp)
            }
            IconButton(onClick = onToggle) { Icon(if (account.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.White.copy(alpha = 0.8f)) }
        }
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text("Số dư khả dụng", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            AnimatedAmountText(amount = account.amount, isHidden = account.isHidden, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            if (account.isLowBalance && !account.isHidden) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Red.copy(alpha = 0.3f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️ Số dư thấp", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(account.name.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(account.purpose.displayName, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
        }
    }
}

@Composable
fun HeaderSectionWithAnim(userEmail: String, isVisible: Boolean) {
    HeaderSection(userEmail)
}

@Composable
fun HeaderSection(userEmail: String) {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11  -> "☀️ Chào buổi sáng"
        in 12..13 -> "🌞 Buổi trưa vui vẻ"
        in 14..17 -> "☀️ Buổi chiều"
        in 18..21 -> "🌇 Buổi tối"
        else      -> "🌙 Khuya rồi"
    }
    val displayName = userEmail.substringBefore("@").replaceFirstChar { it.uppercaseChar() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    displayName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(greeting, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Text(displayName, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
        Box {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.Notifications,
                    null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onAction: (TransactionType?) -> Unit,
    onSavingsAction: () -> Unit,
    onHeldFundsAction: () -> Unit,
    onTransferAction: () -> Unit,
    onNavigate: (String) -> Unit
) {
    data class Action(val label: String, val icon: ImageVector, val gradient: List<Color>)
    val actions = listOf(
        Action("➕ Giao dịch", Icons.Default.Add,             listOf(Color(0xFF3B82F6), Color(0xFF6366F1))),
        Action("🎯 Tiết kiệm", Icons.Default.Savings,         listOf(Color(0xFF10B981), Color(0xFF059669))),
        Action("👥 Ví nhóm",  Icons.Default.Groups,          listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
        Action("🔄 Chuyển tiền", Icons.Default.SwapHoriz,      listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
        Action("📊 Thống kê", Icons.Default.BarChart,         listOf(Color(0xFFEC4899), Color(0xFFDB2777))),
        Action("📝 Kế hoạch",  Icons.AutoMirrored.Filled.EventNote,       listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))),
        Action("💳 Nợ/Vay",    Icons.Default.AccountBalance,  listOf(Color(0xFF9333EA), Color(0xFF7E22CE))),
        Action("📸 Nhật ký",  Icons.Default.PhotoLibrary,    listOf(Color(0xFFEA580C), Color(0xFFDC2626)))
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { action ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                when {
                                    action.label.contains("Giao dịch") -> onAction(null)
                                    action.label.contains("Tiết kiệm") -> onSavingsAction()
                                    action.label.contains("Ví nhóm")  -> onHeldFundsAction()
                                    action.label.contains("Chuyển")    -> onTransferAction()
                                    action.label.contains("Thống kê")  -> onNavigate(Routes.ANALYTICS)
                                    action.label.contains("Kế hoạch")  -> onNavigate(Routes.BUDGET)
                                    action.label.contains("Nợ")        -> onNavigate(Routes.DEBT_LOAN)
                                    action.label.contains("Nhật ký")  -> onNavigate(Routes.PHOTO_DIARY)
                                }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(action.gradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(action.icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            action.label.substringAfter(" "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun TransactionListItem(transaction: FinanceTransaction, onClick: () -> Unit) {
    val (amtColor, signChar) = when (transaction.type) {
        TransactionType.INCOME            -> Pair(Color(0xFF10B981), "+")
        TransactionType.EXPENSE,
        TransactionType.GROUP_PREPAYMENT  -> Pair(Color(0xFFEF4444), "-")
        TransactionType.TRANSFER          -> Pair(Color(0xFF6366F1), "↔")
    }
    val catIcon = getCategoryIcon(transaction.category)
    val catColor = amtColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(catIcon, null, tint = catColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.note.ifBlank { transaction.category },
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                transactionDateFormat.format(transaction.timestamp.toDate()),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            )
        }
        Text(
            "$signChar${formatCurrency(if (transaction.isGroupPrepayment) transaction.personalAmount else transaction.amount)}",
            color = amtColor,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
        )
    }
}

@Composable
fun FundDistributionSection(
    totalManaged: Double,
    personalMoney: Double,
    spendable: Double,
    goalCommitted: Double,
    generalSaved: Double,
    heldFunds: Double,
    onAdjustGeneral: () -> Unit,
    isHiddenGlobal: Boolean,
    onToggleVisible: () -> Unit
) {
    val base = totalManaged.coerceAtLeast(1.0)
    val animSpendable = remember { Animatable(0f) }
    val animGoal      = remember { Animatable(0f) }
    val animGeneral   = remember { Animatable(0f) }
    val animHeld      = remember { Animatable(0f) }
    LaunchedEffect(spendable, goalCommitted, generalSaved, heldFunds) {
        animSpendable.animateTo((spendable / base).toFloat(),      tween(700, easing = FastOutSlowInEasing))
        animGoal.animateTo((goalCommitted / base).toFloat(),       tween(700, easing = FastOutSlowInEasing))
        animGeneral.animateTo((generalSaved / base).toFloat(),     tween(700, easing = FastOutSlowInEasing))
        animHeld.animateTo((heldFunds / base).toFloat(),           tween(700, easing = FastOutSlowInEasing))
    }

    val colorBlue    = PrimaryBlue
    val colorOrange  = Color(0xFFF59E0B)
    val colorGreen   = AccentGreen
    val colorPurple  = Color(0xFF8B5CF6)
    val trackColor   = MaterialTheme.colorScheme.surfaceVariant

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TÀI SẢN CÁ NHÂN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    AnimatedAmountText(amount = personalMoney, isHidden = isHiddenGlobal, color = MaterialTheme.colorScheme.onBackground, fontSize = 32.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onToggleVisible) {
                    Icon(if (isHiddenGlobal) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(24.dp))
            FundProgressBar(
                animSpendable = animSpendable,
                animGoal = animGoal,
                animGeneral = animGeneral,
                animHeld = animHeld,
                colorBlue = colorBlue,
                colorOrange = colorOrange,
                colorGreen = colorGreen,
                colorPurple = colorPurple,
                trackColor = trackColor
            )
            Spacer(Modifier.height(20.dp))
            FundItem("Sử dụng thoải mái", spendable, colorBlue,   Icons.Default.AccountBalanceWallet, isHiddenGlobal)
            FundItem("Mục tiêu tiết kiệm", goalCommitted, colorOrange, Icons.AutoMirrored.Filled.TrendingUp, isHiddenGlobal)
            FundItem("Tiết kiệm chung",   generalSaved,  colorGreen,  Icons.Default.Lock,             isHiddenGlobal)
            FundItem("Ví nhóm",           heldFunds,     colorPurple, Icons.Default.Groups,            isHiddenGlobal)
        }
    }
}

@Composable
private fun FundProgressBar(
    animSpendable: Animatable<Float, AnimationVector1D>,
    animGoal: Animatable<Float, AnimationVector1D>,
    animGeneral: Animatable<Float, AnimationVector1D>,
    animHeld: Animatable<Float, AnimationVector1D>,
    colorBlue: Color,
    colorOrange: Color,
    colorGreen: Color,
    colorPurple: Color,
    trackColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
    ) {
        drawRect(color = trackColor)
        val total = size.width
        var x = 0f
        fun drawSegment(fraction: Float, color: Color) {
            val w = fraction * total
            if (w > 0f) {
                drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(w, size.height))
                x += w
            }
        }
        drawSegment(animSpendable.value, colorBlue)
        drawSegment(animGoal.value,      colorOrange)
        drawSegment(animGeneral.value,   colorGreen)
        drawSegment(animHeld.value,      colorPurple)
    }
}

@Composable
fun FundItem(label: String, amount: Double, color: Color, icon: ImageVector, isHidden: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp)
        }
        AnimatedAmountText(amount, isHidden, MaterialTheme.colorScheme.onBackground, 14.sp, FontWeight.Bold, true)
    }
}
