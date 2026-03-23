package com.example.finfit.health.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.finfit.core.navigation.Routes

fun NavGraphBuilder.healthNavGraph(
    navController: NavHostController,
    userEmail: String
) {
    val onHome = { 
        navController.navigate(Routes.HEALTH_DASHBOARD) {
            popUpTo(Routes.HEALTH_DASHBOARD) { inclusive = true }
        }
    }

    composable(Routes.HEALTH_DASHBOARD) {
        HealthDashboardScreen(
            userEmail = userEmail,
            onNavigate = { route ->
                navController.navigate(route) { launchSingleTop = true }
            }
        )
    }
    composable(Routes.STEP_COUNTER) {
        StepCounterScreen(userEmail, onBack = { navController.popBackStack() }, onHome = onHome)
    }
    composable(Routes.FOOD_SCANNER) {
        FoodScannerScreen(userEmail, onBack = { navController.popBackStack() }, onHome = onHome)
    }
    composable(Routes.HEALTH_STATS) {
        HealthStatsScreen(userEmail, onBack = { navController.popBackStack() }, onHome = onHome)
    }
    composable(Routes.HEALTH_PREDICTION) {
        HealthPredictionScreen(userEmail, onBack = { navController.popBackStack() }, onHome = onHome)
    }
    composable(Routes.HEALTH_LOG) {
        HealthLogScreen(userEmail, onBack = { navController.popBackStack() }, onHome = onHome)
    }
}
