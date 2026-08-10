package com.example.finfit.finance.ui.screens

import com.example.finfit.finance.ui.utils.*
import com.example.finfit.finance.ui.wrappers.*
import com.example.finfit.finance.ui.logic.*
import com.example.finfit.core.logic.InsightEngine

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.derivedStateOf
import com.example.finfit.core.navigation.Routes
import com.example.finfit.finance.model.*
import com.example.finfit.ui.theme.*
import java.util.*

@Composable
fun DashboardScreen(
    userEmail: String,
    wallet: AppUserWallet?,
    transactions: List<FinanceTransaction>,
    goals: List<SavingsGoal>,
    budgets: List<FinanceBudget> = emptyList(),
    onSilentSave: (AppUserWallet) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onUpdateTransaction: (FinanceTransaction) -> Unit,
    onAction: (TransactionType?) -> Unit = {},
    onNavigate: (String) -> Unit = {},
    schedule: List<SpendingScheduleItem> = emptyList(),
    healthState: com.example.finfit.health.model.HealthUiState = com.example.finfit.health.model.HealthUiState()
) {
    var screen by remember { mutableStateOf<DashboardScreenState>(DashboardScreenState.Home) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        when (val s = screen) {
            is DashboardScreenState.Home -> HomeContent(
                userEmail = userEmail,
                wallet = wallet,
                transactions = transactions,
                goals = goals,
                budgets = budgets,
                onSilentSave = onSilentSave,
                onAction = onAction,
                onEditTransaction = { tx -> screen = DashboardScreenState.EditTransaction(tx) },
                onNavigate = onNavigate,
                onSavingsAction = { /* handled in HomeContent */ },
                schedule = schedule,
                healthState = healthState
            )
            is DashboardScreenState.EditTransaction -> EditTransactionScreen(
                transaction = s.transaction,
                onSave = { updated -> onUpdateTransaction(updated); screen = DashboardScreenState.Home },
                onDelete = { id -> onDeleteTransaction(id); screen = DashboardScreenState.Home },
                onBack = { screen = DashboardScreenState.Home },
                onHome = { screen = DashboardScreenState.Home }
            )
        }
    }
}

sealed class DashboardScreenState {
    object Home : DashboardScreenState()
    data class EditTransaction(val transaction: FinanceTransaction) : DashboardScreenState()
}

@Composable
fun HomeContent(
    userEmail: String,
    wallet: AppUserWallet?,
    transactions: List<FinanceTransaction>,
    goals: List<SavingsGoal>,
    budgets: List<FinanceBudget>,
    onSilentSave: (AppUserWallet) -> Unit,
    onAction: (TransactionType?) -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onNavigate: (String) -> Unit,
    onSavingsAction: (() -> Unit)? = null,
    schedule: List<SpendingScheduleItem> = emptyList(),
    healthState: com.example.finfit.health.model.HealthUiState = com.example.finfit.health.model.HealthUiState()
) {
    val context = LocalContext.current
    val isDashboardHidden = wallet?.isTotalBalanceHidden ?: true

    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!visible) visible = true }

    val screenAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "screenAlpha"
    )
    val screenSlide by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "screenSlide"
    )

    val today = remember {
        ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7) + 1
    }
    val todayPlannedItems by remember(schedule) {
        derivedStateOf { schedule.filter { it.dayOfWeek == today } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                alpha = screenAlpha
                translationY = screenSlide
            },
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "header") { HeaderSection(userEmail) }
        item(key = "spacer_top") { Spacer(Modifier.height(20.dp)) }

        item(key = "fund_card") {
            val funds = remember(wallet, goals) {
                val personal = wallet?.totalBalance ?: 0.0
                val goal = goals.sumOf { it.currentAmount }
                val general = wallet?.generalSavings ?: 0.0
                val held = wallet?.totalHeldFunds ?: 0.0
                val total = personal + held
                val spend = (personal - goal - general).coerceAtLeast(0.0)
                CalculatedFunds(personal, goal, general, held, total, spend)
            }
            FundDistributionSection(
                totalManaged = funds.total,
                personalMoney = funds.personal,
                spendable = funds.spendable,
                goalCommitted = funds.goal,
                generalSaved = funds.general,
                heldFunds = funds.held,
                onAdjustGeneral = { onNavigate(Routes.GENERAL_SAVINGS) },
                isHiddenGlobal = isDashboardHidden,
                onToggleVisible = {
                    wallet?.let {
                        onSilentSave(it.copy(isTotalBalanceHidden = !isDashboardHidden))
                        Toast.makeText(context, if (isDashboardHidden) "Hiện số dư tổng" else "Ẩn số dư tổng", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        item(key = "spacer_accounts") { Spacer(Modifier.height(16.dp)) }

        item(key = "accounts") {
            val accounts = wallet?.accounts ?: emptyList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                accounts.forEach { account ->
                    AccountCard(
                        account = account,
                        onToggle = {
                            if (wallet != null) {
                                val updated = wallet.copy(
                                    accounts = wallet.accounts.map {
                                        if (it.id == account.id) it.copy(isHidden = !it.isHidden) else it
                                    }
                                )
                                onSilentSave(updated)
                            }
                        }
                    )
                }
            }
        }

        item(key = "spacer_actions") { Spacer(Modifier.height(24.dp)) }
        item(key = "quick_actions") {
            QuickActionsSection(
                onAction = onAction,
                onSavingsAction = { onNavigate(Routes.GENERAL_SAVINGS) },
                onHeldFundsAction = { onNavigate(Routes.HELD_FUNDS) },
                onTransferAction = { onNavigate(Routes.TRANSFER) },
                onNavigate = onNavigate
            )
        }

        item(key = "insight_banner") {
            val insights = remember(transactions, budgets, goals, healthState) {
                InsightEngine.generateInsights(
                    healthState = healthState,
                    transactions = transactions,
                    budgets = budgets,
                    goals = goals
                ).take(3)
            }
            if (insights.isNotEmpty()) {
                InsightBannerSection(
                    insights = insights,
                    onInsightClick = { route -> route?.let { onNavigate(it) } }
                )
            }
        }
        
        if (todayPlannedItems.isNotEmpty()) {
            item(key = "today_plan") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kế hoạch hôm nay", fontSize = 16.sp, fontWeight = FontWeight.Black)
                        TextButton(onClick = { onNavigate(Routes.FINANCE_PLAN) }) {
                            Text("Xem lịch trình", fontSize = 12.sp, color = PrimaryBlue)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        todayPlannedItems.forEach { plan ->
                            val cat = EXPENSE_CATEGORIES.find { it.label == plan.category } ?: EXPENSE_CATEGORIES.last()
                            Card(
                                modifier = Modifier.width(160.dp).clickable { onNavigate("${Routes.ADD}?type=${TransactionType.EXPENSE.name}") },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = cat.color.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, cat.color.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(cat.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cat.color)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(formatCurrency(plan.amount), fontSize = 15.sp, fontWeight = FontWeight.Black)
                                    if (plan.note.isNotBlank()) {
                                        Text(plan.note, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "spacer_plan") { Spacer(Modifier.height(12.dp)) }

        item(key = "budget_progress") {
            PlanProgressSection(transactions, budgets, onNavigate)
        }
        item(key = "spacer_savings") { Spacer(Modifier.height(32.dp)) }

        item(key = "savings_goals") { SavingsGoalsSection(goals, onNavigate) }
        item(key = "spacer_recent") { Spacer(Modifier.height(32.dp)) }

        item(key = "recent_tx") { RecentTransactionsSection(transactions, onEditTransaction, onNavigate) }
        item(key = "spacer_bottom") { Spacer(Modifier.height(100.dp)) }
    }
}
