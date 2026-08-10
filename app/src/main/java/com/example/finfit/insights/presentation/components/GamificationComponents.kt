package com.example.finfit.insights.presentation.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.insights.domain.model.*

@Composable
fun LevelHeaderHeroCard(
    profile: UserGamificationProfile,
    modifier: Modifier = Modifier
) {
    val tier = profile.levelTier
    val primaryColor = Color(tier.levelColorHex)
    val animatedProgress by animateFloatAsState(
        targetValue = profile.levelProgressRatio,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "xp_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = primaryColor.copy(alpha = 0.3f), spotColor = primaryColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
        border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background subtle gradient aura
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )

            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Badge Level Avatar
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF222226), Color(0xFF16161A))))
                                .border(2.dp, primaryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tier.badgeIcon, fontSize = 28.sp)
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("CẤP ${tier.levelNumber}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = primaryColor, letterSpacing = 1.sp)
                                Surface(
                                    color = primaryColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("TIER ${tier.levelNumber}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text(tier.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // XP Pill
                    Surface(
                        color = Color(0xFF1E1E24),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⚡", fontSize = 14.sp)
                            Text("${profile.currentXp} XP", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // XP Progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tiến độ thăng hạng", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("Còn ${profile.nextLevelRemainingXp} XP để lên cấp kế", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryColor)
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.7f), primaryColor)))
                    )
                }
            }
        }
    }
}

@Composable
fun StreakSectionCard(
    streakData: StreakData,
    onUseFreeze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔥", fontSize = 20.sp)
                    Text("Chuỗi Hoạt Động (Streaks)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Freeze Button
                Surface(
                    onClick = onUseFreeze,
                    shape = RoundedCornerShape(12.dp),
                    color = if (streakData.isFrozenToday) Color(0xFF1E88E5).copy(alpha = 0.25f) else Color(0xFF1E1E24),
                    border = BorderStroke(1.dp, if (streakData.isFrozenToday) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("❄️", fontSize = 12.sp)
                        Text(
                            if (streakData.isFrozenToday) "Đã đóng băng" else "Freeze (${streakData.freezesRemaining}/2)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (streakData.isFrozenToday) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 4 Streak Items Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StreakChip(
                    title = "Ghi chép",
                    count = "${streakData.loggingStreak} ngày",
                    icon = "📝",
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                StreakChip(
                    title = "Ngân sách",
                    count = "${streakData.budgetStreakWeeks} tuần",
                    icon = "🛡️",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StreakChip(
                    title = "Sức khỏe",
                    count = "${streakData.healthStreak} ngày",
                    icon = "🏃‍♂️",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                StreakChip(
                    title = "Double Fit",
                    count = "${streakData.doubleSynergyStreak} ngày",
                    icon = "⚡",
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StreakChip(
    title: String,
    count: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1C1C20),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 18.sp)
            Text(count, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text(title, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AnonymousBenchmarkCard(
    benchmarkPercent: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A2332),
        border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF64B5F6).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📊", fontSize = 22.sp)
            }

            Column(Modifier.weight(1f)) {
                Text(
                    "Top ${100 - benchmarkPercent}% Kỷ Luật Xuất Sắc",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90CAF9)
                )
                Text(
                    "Bạn đang duy trì thói quen tài chính & dinh dưỡng vượt qua $benchmarkPercent% người dùng FinFit!",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun BadgeGridSection(
    badges: List<GamificationBadge>,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<BadgeCategory?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏅 Bộ Sưu Tập Huy Hiệu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            val unlockedCount = badges.count { it.isUnlocked }
            Text("$unlockedCount/${badges.size} đã mở khóa", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF81C784))
        }

        Spacer(Modifier.height(12.dp))

        // Category Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryFilterChip(
                label = "Tất cả",
                isSelected = selectedCategory == null,
                onClick = { selectedCategory = null }
            )
            BadgeCategory.values().forEach { cat ->
                CategoryFilterChip(
                    label = "${cat.icon} ${cat.title}",
                    isSelected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val filteredBadges = if (selectedCategory == null) badges else badges.filter { it.category == selectedCategory }

        // Badge Items
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredBadges.forEach { badge ->
                BadgeItemCard(badge = badge)
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF1C1C20),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.1f))
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF93C5FD) else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun BadgeItemCard(badge: GamificationBadge) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (badge.isUnlocked) Color(0xFF17171A) else Color(0xFF121214),
        border = BorderStroke(
            1.dp,
            if (badge.isUnlocked) Color(0xFFFFD700).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) Color(0xFF26262B) else Color(0xFF19191C))
                    .border(
                        1.5.dp,
                        if (badge.isUnlocked) Color(0xFFFFD700) else Color.White.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    badge.icon,
                    fontSize = 24.sp,
                    color = if (badge.isUnlocked) Color.Unspecified else Color.White.copy(alpha = 0.3f)
                )
            }

            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        badge.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isUnlocked) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                    Surface(
                        color = Color(0xFF222228),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "+${badge.xpReward} XP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(3.dp))
                Text(
                    badge.description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    lineHeight = 15.sp
                )

                Spacer(Modifier.height(6.dp))
                if (badge.isUnlocked) {
                    Text(
                        "✨ Đã đạt ${badge.unlockedDateStr ?: ""}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD700)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { badge.progressRatio },
                            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF3B82F6),
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Text(
                            badge.currentProgressText,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeListSection(
    challenges: List<GamificationChallenge>,
    onClaimReward: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("⏳", fontSize = 18.sp)
                Text("Thử Thách Có Hạn", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            challenges.forEach { challenge ->
                ChallengeCardItem(challenge = challenge, onClaim = { onClaimReward(challenge.id) })
            }
        }
    }
}

@Composable
fun ChallengeCardItem(
    challenge: GamificationChallenge,
    onClaim: () -> Unit
) {
    val progress = (challenge.currentCount.toFloat() / challenge.targetCount).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141416),
        border = BorderStroke(1.dp, if (challenge.isCompleted) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(challenge.icon, fontSize = 24.sp)
                    Column {
                        Text(challenge.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            if (challenge.isCompleted) "🎉 Đã hoàn thành!" else "Còn ${challenge.deadlineDaysLeft} ngày nữa",
                            fontSize = 11.sp,
                            color = if (challenge.isCompleted) Color(0xFF81C784) else Color(0xFFF59E0B)
                        )
                    }
                }

                Surface(
                    color = Color(0xFF222228),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "+${challenge.xpReward} XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF64B5F6),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(challenge.description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), lineHeight = 16.sp)

            Spacer(Modifier.height(12.dp))

            // Progress and Claim Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tiến độ: ${challenge.currentCount}/${challenge.targetCount}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (challenge.isCompleted) Color(0xFF81C784) else Color(0xFF3B82F6),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                if (challenge.isCompleted) {
                    Button(
                        onClick = onClaim,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Nhận Thưởng", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
