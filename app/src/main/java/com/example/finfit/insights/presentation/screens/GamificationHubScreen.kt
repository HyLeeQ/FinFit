package com.example.finfit.insights.presentation.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finfit.insights.presentation.components.*
import com.example.finfit.insights.presentation.viewmodel.GamificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationHubScreen(
    userId: String,
    viewModel: GamificationViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.loadData(userId)
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0D0D0E),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎮", fontSize = 20.sp)
                        Text(
                            "Trung Tâm Động Lực",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D0E))
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF64B5F6))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Level & XP Hero Card
                LevelHeaderHeroCard(profile = uiState.profile)

                // 2. Streak Section & Freeze Button
                StreakSectionCard(
                    streakData = uiState.profile.streakData,
                    onUseFreeze = { viewModel.useStreakFreeze(userId) }
                )

                // 3. Anonymous Discipline Benchmark
                AnonymousBenchmarkCard(benchmarkPercent = uiState.profile.anonymousDisciplineBenchmark)

                // 4. Time-limited Challenges
                ChallengeListSection(
                    challenges = uiState.challenges,
                    onClaimReward = { challengeId -> viewModel.claimChallenge(userId, challengeId) }
                )

                // 5. Badges Collection
                BadgeGridSection(badges = uiState.badges)

                // 6. Settings Toggle Row (Tùy chọn tắt/bật Gamification)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141416),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Bật hệ thống động lực & Huy hiệu", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Tắt nếu bạn chỉ muốn giao diện số liệu tối giản", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = uiState.profile.isGamificationEnabled,
                            onCheckedChange = { enabled -> viewModel.toggleGamification(userId, enabled) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF3B82F6)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
