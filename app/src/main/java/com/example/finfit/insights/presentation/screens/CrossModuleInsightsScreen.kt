package com.example.finfit.insights.presentation.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.finance.ui.utils.formatCurrency
import com.example.finfit.insights.domain.model.*
import com.example.finfit.insights.presentation.viewmodel.CrossModuleInsightsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossModuleInsightsScreen(
    onBack: () -> Unit,
    viewModel: CrossModuleInsightsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Phân Tích Sức Khỏe ↔ Tài Chính",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Liên kết Dinh dưỡng, Vận động & Dòng tiền",
                            fontSize = 12.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF0E0E0E)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Tab Selector ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tabs = listOf("📊 Biểu Đồ Đối Chiếu", "🍲 Tiết Kiệm Tự Nấu", "🏆 Thử Thách & Badges")
                    tabs.forEachIndexed { index, label ->
                        val isSelected = uiState.selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF2E7D32) else Color.Transparent)
                                .clickable { viewModel.selectTab(index) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFFB0B0B0)
                            )
                        }
                    }
                }
            }

            // ── Smart Alert Banner ────────────────────────────────
            uiState.activeAlert?.let { alert ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A1E)),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💡", fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF81C784))
                                Text(alert.message, fontSize = 12.sp, color = Color(0xFFE0E0E0))
                                Spacer(Modifier.height(2.dp))
                                Text(alert.recommendation, fontSize = 11.sp, color = Color(0xFFA5D6A7), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                    }
                }
            }

            // ── Tab 0: Biểu Đồ Đối Chiếu & Chi Phí / Calo ─────────
            if (uiState.selectedTab == 0) {
                item {
                    DualChartCard(uiState.weeklySummary)
                }

                item {
                    CostPerCalorieCard(uiState.weeklySummary)
                }

                item {
                    DiningVsHomeCard(uiState.weeklySummary)
                }
            }

            // ── Tab 1: Quỹ Tiết Kiệm Tự Nấu Ăn ─────────────────────
            if (uiState.selectedTab == 1) {
                item {
                    HealthySavingsHeroCard(uiState.piggybank)
                }

                item {
                    CookingStreakCard(uiState.piggybank)
                }
            }

            // ── Tab 2: Thử Thách & Badges Liên Module ─────────────
            if (uiState.selectedTab == 2) {
                item {
                    Text(
                        "🎯 Thử Thách Tuần: Sống Khỏe & Giàu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                items(uiState.challenges) { challenge ->
                    ChallengeCard(challenge)
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "🏅 Huy Hiệu Thành Tựu Liên Module",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                items(uiState.badges) { badge ->
                    BadgeCard(badge)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DualChartCard(summary: CrossModuleWeeklySummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = BorderStroke(1.dp, Color(0xFF282828)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📊 Đối Chiếu Chi Tiêu vs Calo Tuần", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(Modifier.width(4.dp))
                    Text("Tiền", fontSize = 11.sp, color = Color(0xFFB0B0B0))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF9800)))
                    Spacer(Modifier.width(4.dp))
                    Text("Calo", fontSize = 11.sp, color = Color(0xFFB0B0B0))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bars representation for 7 days
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                summary.dailyMetrics.forEach { metric ->
                    val maxExpense = 150000.0
                    val maxCal = 3000.0
                    val expenseHeight = ((metric.foodExpense / maxExpense) * 90).coerceIn(10.0, 90.0).dp
                    val calHeight = ((metric.caloriesIn / maxCal) * 90).coerceIn(10.0, 90.0).dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Expense Bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(expenseHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFF4CAF50))
                            )
                            // Calorie Bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(calHeight)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFFFF9800))
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(metric.dayLabel, fontSize = 11.sp, color = if (metric.dayOfWeek in 6..7) Color(0xFFFFCC80) else Color(0xFF888888))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF282828))
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡 Insight:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF81C784))
                Spacer(Modifier.width(6.dp))
                Text(summary.weeklyPatternInsight, fontSize = 12.sp, color = Color(0xFFCCCCCC))
            }
        }
    }
}

@Composable
private fun CostPerCalorieCard(summary: CrossModuleWeeklySummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = BorderStroke(1.dp, Color(0xFF282828)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("⚡ Hiệu Suất Chi Phí Dinh Dưỡng", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cost per 1000 kcal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222222))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Chi phí / 1.000 kcal", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatCurrency(summary.costPerThousandCalories),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF81C784)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Tiêu chuẩn: ~40k - 50k", fontSize = 10.sp, color = Color(0xFF757575))
                    }
                }

                // Cost per 100g protein
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222222))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Chi phí / 100g Protein", fontSize = 11.sp, color = Color(0xFF9E9E9E))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatCurrency(summary.costPerHundredGramProtein),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF64B5F6)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("Tiêu chuẩn: ~100k - 130k", fontSize = 10.sp, color = Color(0xFF757575))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiningVsHomeCard(summary: CrossModuleWeeklySummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = BorderStroke(1.dp, Color(0xFF282828)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🍲 Tỷ Trọng Ăn Ngoài vs Tự Nấu", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            Spacer(Modifier.height(12.dp))

            val diningRatio = summary.diningOutVsHomeRatio.toFloat().coerceIn(0f, 1f)
            val homeRatio = 1f - diningRatio

            // Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .weight(if (diningRatio > 0) diningRatio else 0.5f)
                        .fillMaxHeight()
                        .background(Color(0xFFE53935))
                )
                Box(
                    modifier = Modifier
                        .weight(if (homeRatio > 0) homeRatio else 0.5f)
                        .fillMaxHeight()
                        .background(Color(0xFF43A047))
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE53935)))
                    Spacer(Modifier.width(6.dp))
                    Text("Ăn ngoài: ${formatCurrency(summary.diningOutExpense)} (${(diningRatio * 100).toInt()}%)", fontSize = 11.sp, color = Color(0xFFE0E0E0))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF43A047)))
                    Spacer(Modifier.width(6.dp))
                    Text("Tự nấu: ${formatCurrency(summary.homeCookingExpense)} (${(homeRatio * 100).toInt()}%)", fontSize = 11.sp, color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}

@Composable
private fun HealthySavingsHeroCard(piggybank: HealthySavingsPiggybank) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162516)),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF81C784), Color(0xFF2E7D32))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🐷", fontSize = 32.sp)
            }

            Spacer(Modifier.height(12.dp))
            Text("Quỹ Ảo Tiết Kiệm Tự Nấu Ăn", fontSize = 13.sp, color = Color(0xFFA5D6A7), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                formatCurrency(piggybank.totalVirtualSaved),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF81C784)
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "Được tính dựa trên chênh lệch giữa ${piggybank.homeCookedMealsCount} bữa tự nấu và chi phí ăn ngoài trung bình (~${formatCurrency(piggybank.averageDiningOutCost)}/bữa).",
                fontSize = 11.sp,
                color = Color(0xFFB0B0B0),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun CookingStreakCard(piggybank: HealthySavingsPiggybank) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = BorderStroke(1.dp, Color(0xFF282828)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("🔥 Chuỗi Tự Nấu Ăn", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Bạn đã duy trì tự nấu ăn ${piggybank.streakDays} ngày liên tiếp!", fontSize = 12.sp, color = Color(0xFFB0B0B0))
            }
            Text("${piggybank.streakDays} ngày", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFFFF9800))
        }
    }
}

@Composable
private fun ChallengeCard(challenge: CrossModuleChallenge) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        border = BorderStroke(1.dp, if (challenge.isCompleted) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color(0xFF2A2A2A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(challenge.icon, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(challenge.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
                if (challenge.isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2E7D32))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("HOÀN THÀNH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(challenge.description, fontSize = 12.sp, color = Color(0xFFB0B0B0))
            Spacer(Modifier.height(10.dp))

            // Progress Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bước chân TB: ${challenge.currentStepsAvg}/${challenge.targetSteps}", fontSize = 11.sp, color = Color(0xFF81C784))
                Text("Ăn ngoài: ${challenge.currentDiningOutCount}/${challenge.maxDiningOutCount} lần", fontSize = 11.sp, color = Color(0xFFFFB74D))
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: CrossModuleBadge) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (badge.isUnlocked) Color(0xFF1B241B) else Color(0xFF161616)),
        border = BorderStroke(1.dp, if (badge.isUnlocked) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color(0xFF282828)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) Color(0xFF2E7D32) else Color(0xFF282828)),
                contentAlignment = Alignment.Center
            ) {
                Text(badge.icon, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(badge.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (badge.isUnlocked) Color.White else Color(0xFF757575))
                Text(badge.description, fontSize = 11.sp, color = if (badge.isUnlocked) Color(0xFFB0B0B0) else Color(0xFF616161))
            }
            if (badge.isUnlocked) {
                badge.unlockedDateStr?.let {
                    Text(it, fontSize = 10.sp, color = Color(0xFF81C784))
                }
            } else {
                Text("🔒 Khóa", fontSize = 10.sp, color = Color(0xFF757575))
            }
        }
    }
}
