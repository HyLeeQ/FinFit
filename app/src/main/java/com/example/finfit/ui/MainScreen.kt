package com.example.finfit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.finfit.core.navigation.AppMode
import com.example.finfit.core.navigation.BottomNavItem
import com.example.finfit.core.navigation.Routes
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.ui.DashboardWithData
import com.example.finfit.finance.ui.SavingsGoalWithData
import com.example.finfit.finance.ui.WalletManagementWithData
import com.example.finfit.health.ui.HealthDashboardScreen
import com.example.finfit.health.ui.StepCounterScreen
import com.example.finfit.health.ui.FoodScannerScreen
import com.example.finfit.health.ui.HealthStatsScreen
import com.example.finfit.health.ui.HealthPredictionScreen
import com.example.finfit.health.ui.HealthLogScreen
import kotlin.math.roundToInt

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
    var appMode by remember { mutableStateOf(AppMode.FINANCE) }

    // Make sure initial Tab syncs with app mode if needed
    LaunchedEffect(appMode) {
        val targetRoute = if (appMode == AppMode.FINANCE) Routes.DASHBOARD else Routes.HEALTH_DASHBOARD
        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            TopModeSwitcher(appMode = appMode, onModeChange = { appMode = it })
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController, 
                appMode = appMode,
                onTabSelected = onTabSelected
            )
        },
        floatingActionButton = {
            if (appMode == AppMode.FINANCE) {
                FloatingActionButton(
                    onClick = { onAction(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.offset(y = 50.dp).size(64.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm", modifier = Modifier.size(32.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = initialTab,
                modifier = Modifier.padding(innerPadding)
            ) {
                // Finance Routes
                composable(Routes.DASHBOARD) {
                    DashboardWithData(
                        userEmail = userEmail,
                        firestoreRepository = firestoreRepository,
                        refreshTrigger = refreshTrigger,
                        onLogout = onLogout,
                        onAction = onAction,
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(Routes.FINANCE_WALLET) {
                    WalletManagementWithData(
                        firestoreRepository = firestoreRepository,
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(Routes.FINANCE_PLAN) {
                    // Placeholder for future feature
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tính năng Kế hoạch chi tiêu sẽ phát triển sau", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                composable(Routes.SAVINGS_GOALS) {
                    SavingsGoalWithData(
                        firestoreRepository = firestoreRepository,
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Assistant screen
                composable(Routes.ASSISTANT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background), 
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Trợ lý AI ✨", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.height(8.dp))
                            Text("Tính năng hội thoại đang được phát triển...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Health Routes
                composable(Routes.HEALTH_DASHBOARD) {
                    HealthDashboardScreen(
                        userEmail = userEmail,
                        onNavigate = { route ->
                            navController.navigate(route) { launchSingleTop = true }
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
                
                // Common
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        email = userEmail,
                        themeMode = themeMode,
                        onThemeChange = onThemeChange,
                        onLogout = onLogout
                    )
                }
            }
            
            // Floating AI Bubble over everything
            AIFloatingBubble(onClick = { 
                navController.navigate(Routes.ASSISTANT) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            })
        }
    }
}

@Composable
fun TopModeSwitcher(appMode: AppMode, onModeChange: (AppMode) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Row {
                    ModeSwitchButton(
                        text = "Tài chính",
                        isSelected = appMode == AppMode.FINANCE,
                        onClick = { onModeChange(AppMode.FINANCE) }
                    )
                    Space()
                    ModeSwitchButton(
                        text = "Sức khỏe",
                        isSelected = appMode == AppMode.HEALTH,
                        onClick = { onModeChange(AppMode.HEALTH) }
                    )
                }
            }
        }
    }
}

@Composable
fun Space() { Spacer(modifier = Modifier.width(4.dp)) }

@Composable
fun ModeSwitchButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun AIFloatingBubble(onClick: () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(200f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(60.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDragging) 1f else 0.6f))
            .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "AI Assistant",
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, appMode: AppMode, onTabSelected: (String) -> Unit = {}) {
    val items = if (appMode == AppMode.FINANCE) {
        listOf(
            BottomNavItem.FinanceHome,
            BottomNavItem.FinanceWallet,
            null, // Placeholder for FAB
            BottomNavItem.FinancePlan,
            BottomNavItem.Profile
        )
    } else {
        listOf(
            BottomNavItem.HealthHome,
            BottomNavItem.HealthFeatures,
            BottomNavItem.Profile
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            if (item == null) {
                NavigationBarItem(
                    icon = {}, label = { Text("") }, selected = false, onClick = {}, enabled = false
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
