package com.example.finfit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.finfit.core.navigation.BottomNavItem
import com.example.finfit.core.navigation.Routes
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.ui.DashboardWithData
import com.example.finfit.finance.ui.AddTransactionWithData
import com.example.finfit.finance.ui.FinanceScreen
import com.example.finfit.health.ui.*

@Composable
fun MainScreen(
    userEmail: String,
    firestoreRepository: FirestoreRepository,
    refreshTrigger: Int,
    onTransactionSaved: () -> Unit,
    onLogout: () -> Unit,
    onAction: (TransactionType?) -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    onAction(null)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
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
            startDestination = Routes.DASHBOARD,
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
            composable(Routes.FINANCE) {
                FinanceScreen()
            }
            // HÀM ADD ĐÃ ĐƯỢC CHUYỂN RA NAVHOST NGOÀI (MainActivity) 
            // ĐỂ XỬ LÝ ĐƯỜNG DẪN TOÀN CỤC VÀ ĐỒNG BỘ NAVIGATION
            /*
            composable(
                route = "${Routes.ADD}?type={type}",
                ...
            ) { ... }
            */
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
                ProfileScreen(userEmail, onLogout)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Finance,
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
                // Empty space for the center FAB
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
                        val isHealthTab = item.route == Routes.HEALTH_DASHBOARD
                        val isAlreadyHealth = currentDestination?.hierarchy?.any { it.route == Routes.HEALTH_DASHBOARD || it.route == Routes.STEP_COUNTER || it.route == Routes.FOOD_SCANNER || it.route == Routes.HEALTH_STATS || it.route == Routes.HEALTH_PREDICTION || it.route == Routes.HEALTH_LOG } == true
                        
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            // Nếu đang bấm vào Health tab thì luôn load mới, không khôi phục luồng cũ
                            restoreState = item.route != Routes.HEALTH_DASHBOARD
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = "Tính năng đang phát triển", fontSize = 16.sp)
        }
    }
}
