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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.navigation.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.finfit.core.navigation.AppMode
import com.example.finfit.core.navigation.BottomNavItem
import com.example.finfit.core.navigation.Routes
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.ui.navigation.financeNavGraph
import com.example.finfit.health.ui.*
import com.example.finfit.ui.theme.PrimaryBlue
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    userEmail: String,
    firestoreRepository: FirestoreRepository,
    refreshTrigger: Int,
    onTransactionSaved: () -> Unit,
    onLogout: () -> Unit,
    themeMode: com.example.finfit.data.local.ThemeMode,
    onThemeChange: (com.example.finfit.data.local.ThemeMode) -> Unit,
    initialTab: String = Routes.DASHBOARD,
    onTabSelected: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    var appMode by remember { mutableStateOf(AppMode.FINANCE) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tự động đồng bộ appMode nếu người dùng nhấn nút Back hệ thống
    LaunchedEffect(currentRoute) {
        if (currentRoute == Routes.DASHBOARD) {
            appMode = AppMode.FINANCE
        } else if (currentRoute == Routes.HEALTH_DASHBOARD) {
            appMode = AppMode.HEALTH
        }
    }

    // Xác định xem có đang ở trang con của Sức Khoẻ không (theo yêu cầu chỉ cập nhật Health)
    val isHealthSubScreen = currentRoute in listOf(
        Routes.STEP_COUNTER,
        Routes.WATER_TRACKER,
        Routes.FOOD_SCANNER,
        Routes.HEALTH_PREDICTION,
        Routes.HEALTH_LOG
    )
    val showBars = !isHealthSubScreen

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
            if (showBars) {
                TopModeSwitcher(appMode = appMode, onModeChange = { appMode = it })
            }
        },
        bottomBar = {
            if (showBars) {
                BottomNavigationBar(
                    navController = navController, 
                    appMode = appMode,
                    onTabSelected = onTabSelected
                )
            }
        },
        floatingActionButton = {
            if (appMode == AppMode.FINANCE) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.ADD) },
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
                financeNavGraph(
                    navController = navController,
                    userEmail = userEmail,
                    firestoreRepository = firestoreRepository,
                    refreshTrigger = refreshTrigger,
                    onLogout = onLogout,
                    onAction = { actionType ->
                        val route = if (actionType != null) "${Routes.ADD}?type=${actionType.name}" else Routes.ADD
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                )
                
                // Health Routes
                healthNavGraph(
                    navController = navController,
                    userEmail = userEmail
                )

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
    // Smaller, elegant pill switcher with sliding animation
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(42.dp)
                .width(220.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
        ) {
            // Sliding indicator with spring physics
            val indicatorOffset by animateDpAsState(
                targetValue = if (appMode == AppMode.FINANCE) 2.dp else 110.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "IndicatorOffset"
            )

            Box(
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .offset(x = indicatorOffset)
                    .width(108.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .shadow(4.dp, CircleShape)
            )

            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                ModeSwitchButton(
                    modifier = Modifier.weight(1f),
                    text = "Tài chính",
                    isSelected = appMode == AppMode.FINANCE,
                    onClick = { onModeChange(AppMode.FINANCE) }
                )
                ModeSwitchButton(
                    modifier = Modifier.weight(1f),
                    text = "Sức khỏe",
                    isSelected = appMode == AppMode.HEALTH,
                    onClick = { onModeChange(AppMode.HEALTH) }
                )
            }
        }
    }
}

@Composable
fun Space() { Spacer(modifier = Modifier.width(4.dp)) }

@Composable
fun ModeSwitchButton(modifier: Modifier = Modifier, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(400),
        label = "ContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null 
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
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
