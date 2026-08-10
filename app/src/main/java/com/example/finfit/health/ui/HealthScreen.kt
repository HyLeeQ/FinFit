package com.example.finfit.health.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.finfit.health.repository.HealthViewModel
import com.example.finfit.health.ui.cards.*
import com.example.finfit.core.navigation.Routes

// ─── Cards đã được chuyển sang: health/ui/cards/HealthCards.kt ────────────────
// HealthWaterMiniCard, HealthStepActivityCard, HealthSleepArcCard,
// HealthEnergyBalanceCard, HealthFoodScanCard, HealthDailyScoreCard,
// InsightItem, ScoreMetricRow

// ─── CÁC THÀNH PHẦN CHUNG ──────────────────────────────────────────────────────

@Composable
fun HealthHeaderSection(
    title: String,
    userEmail: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    actionIcon: ImageVector? = null,
    onActionClick: () -> Unit = {},
    actionIcon2: ImageVector? = null,
    onActionClick2: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            } else {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF262626)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF64b5f6))
                }
            }
            Spacer(modifier = Modifier.width(if (showBackButton) 8.dp else 12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = (-0.5).sp)
                Text("Chào buổi sáng, ${userEmail.split("@")[0]}", color = Color(0xFFadaaaa), fontSize = 12.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (actionIcon2 != null) {
                IconButton(onClick = onActionClick2) {
                    Icon(actionIcon2, contentDescription = "Action 2", tint = Color(0xFFff716c))
                }
            }
            if (actionIcon != null) {
                IconButton(onClick = onActionClick) {
                    Icon(actionIcon, contentDescription = "Action", tint = Color(0xFF64b5f6))
                }
            }
            IconButton(onClick = onHomeClick) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White)
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF262626)))
        }
    }
}

// ─── Placeholder Screen ─────────────────────────────────────────────────────────

@Composable
fun HealthPlaceholderScreen(
    userEmail: String,
    title: String,
    showBackButton: Boolean = true,
    onBack: () -> Unit = {},
    onHome: () -> Unit = onBack
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0e0e0e))) {
        HealthHeaderSection(
            title = title,
            userEmail = userEmail,
            showBackButton = showBackButton,
            onBackClick = onBack,
            onHomeClick = onHome
        )
        Box(
            modifier = Modifier.fillMaxSize().weight(1f).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Tính năng đang phát triển", fontSize = 16.sp, color = Color(0xFFadaaaa))
            }
        }
    }
}

@Composable fun HealthStatsScreen(userEmail: String, onBack: () -> Unit, onHome: () -> Unit = onBack) =
    HealthPlaceholderScreen(userEmail, "Phân tích sức khỏe", showBackButton = false, onBack = onBack, onHome = onHome)

@Composable fun HealthPredictionScreen(userEmail: String, onBack: () -> Unit, onHome: () -> Unit = onBack) =
    HealthPlaceholderScreen(userEmail, "Dự báo sức khỏe", onBack = onBack, onHome = onHome)

@Composable fun HealthLogScreen(userEmail: String, onBack: () -> Unit, onHome: () -> Unit = onBack) =
    HealthPlaceholderScreen(userEmail, "Nhật ký sức khỏe", onBack = onBack, onHome = onHome)

// ─── Data class cho card ─────────────────────────────────────────────────────────

data class HealthCardItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

// ════════════════════════════════════════════════════════════════════════════════
// HEALTH DASHBOARD SCREEN
// ════════════════════════════════════════════════════════════════════════════════

@Composable
fun HealthDashboardScreen(
    userEmail: String,
    onNavigate: (String) -> Unit,
    healthViewModel: HealthViewModel = viewModel(),
    wallet: com.example.finfit.finance.model.AppUserWallet? = null,
    transactions: List<com.example.finfit.finance.model.FinanceTransaction> = emptyList(),
    goals: List<com.example.finfit.finance.model.SavingsGoal> = emptyList()
) {
    val uiState by healthViewModel.healthUiState.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(Color(0xFF0e0e0e)).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting
        item(span = { GridItemSpan(2) }) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "Chào buổi sáng.",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Text(text = "Bắt đầu ngày mới năng động và khỏe mạnh.", fontSize = 16.sp, color = Color(0xFFadaaaa))
            }
        }

        // Daily Score
        item(span = { GridItemSpan(2) }) {
            HealthDailyScoreCard(uiState = uiState)
        }

        // Water
        item(span = { GridItemSpan(1) }) {
            HealthWaterMiniCard(
                consumedMl = uiState.waterConsumedMl,
                goalMl = uiState.waterGoalMl,
                onAddWater = { amount -> healthViewModel.logWater(amountMl = amount, goalMl = uiState.waterGoalMl) },
                onClick = { onNavigate(Routes.WATER_TRACKER) }
            )
        }

        // Steps & Activity
        item(span = { GridItemSpan(1) }) {
            HealthStepActivityCard(
                steps = uiState.steps,
                stepGoal = uiState.stepGoal,
                caloriesOut = uiState.caloriesOut,
                onClick = { onNavigate("stepCounter") }
            )
        }

        // Energy Balance
        item(span = { GridItemSpan(1) }) {
            HealthEnergyBalanceCard(netCalorieBalance = uiState.netCalorieBalance)
        }

        // Sleep
        item(span = { GridItemSpan(1) }) {
            HealthSleepArcCard(
                sleepHours = uiState.sleepHours,
                onClick = { onNavigate(Routes.SLEEP_SCHEDULE) }
            )
        }

        // Food Scan
        item(span = { GridItemSpan(2) }) {
            HealthFoodScanCard(onClick = { onNavigate("food_scanner") })
        }

        // Finance Summary Mini Card
        item(span = { GridItemSpan(2) }) {
            FinanceSummaryMiniCard(
                wallet = wallet,
                transactions = transactions,
                goals = goals,
                onClick = { onNavigate(Routes.DASHBOARD) }
            )
        }
    }
}
