package com.example.finfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.finfit.core.navigation.BottomNavItem
import com.example.finfit.core.navigation.Routes
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.ui.DashboardWithData
import com.example.finfit.health.ui.HealthDashboardScreen
import com.example.finfit.health.ui.StepCounterScreen
import com.example.finfit.health.ui.FoodScannerScreen
import com.example.finfit.health.ui.HealthStatsScreen
import com.example.finfit.health.ui.HealthPredictionScreen
import com.example.finfit.health.ui.HealthLogScreen
import com.example.finfit.ui.ProfileScreen

@Composable
fun MainScreen(
    userEmail: String,
    firestoreRepository: FirestoreRepository,
    refreshTrigger: Int,
    onTransactionSaved: () -> Unit,
    onLogout: () -> Unit,
    onAction: (TransactionType?) -> Unit,
    themeMode: com.example.finfit.data.local.ThemeMode,
    onThemeChange: (com.example.finfit.data.local.ThemeMode) -> Unit,
    initialTab: String = Routes.DASHBOARD,
    onTabSelected: (String) -> Unit = {}
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, onTabSelected = onTabSelected)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    onAction(null)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .offset(y = 50.dp)
                    .size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm", modifier = Modifier.size(32.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialTab,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardWithData(
                    userEmail = userEmail,
                    firestoreRepository = firestoreRepository,
                    refreshTrigger = refreshTrigger,
                    onLogout = onLogout,
                    onAction = onAction
                )
            }
            composable(Routes.ASSISTANT) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Text(
                        "Trợ lý AI FinFit",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        "Chào bạn! Tôi là trợ lý tài chính thông minh của bạn. Tôi có thể giúp bạn phân tích chi tiêu và tư vấn tiết kiệm.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    Spacer(Modifier.height(48.dp))
                    
                    Button(
                        onClick = { /* Phát triển sau */ },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Bắt đầu trò chuyện", fontWeight = FontWeight.Bold)
                    }
                }
            }
            composable(Routes.HEALTH_DASHBOARD) {
                HealthDashboardScreen(
                    userEmail = userEmail,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.STEP_COUNTER) {
                StepCounterScreen(userEmail) { navController.popBackStack() }
            }
            composable(Routes.FOOD_SCANNER) {
                FoodScannerScreen(userEmail) { navController.popBackStack() }
            }
            composable(Routes.HEALTH_STATS) {
                HealthStatsScreen(userEmail) { navController.popBackStack() }
            }
            composable(Routes.HEALTH_PREDICTION) {
                HealthPredictionScreen(userEmail) { navController.popBackStack() }
            }
            composable(Routes.HEALTH_LOG) {
                HealthLogScreen(userEmail) { navController.popBackStack() }
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    email = userEmail,
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, onTabSelected: (String) -> Unit = {}) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Assistant,
        null, // Placeholder for FAB
        BottomNavItem.Health,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            if (item == null) {
                NavigationBarItem(
                    icon = {},
                    label = { Text("") },
                    selected = false,
                    onClick = {},
                    enabled = false
                )
            } else {
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label, fontSize = 10.sp) },
                    selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                    onClick = {
                        onTabSelected(item.route)
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
