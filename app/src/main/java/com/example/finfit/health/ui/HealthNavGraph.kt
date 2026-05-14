package com.example.finfit.health.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.finfit.core.navigation.Routes

fun NavGraphBuilder.healthNavGraph(
    navController: NavHostController,
    userEmail: String
) {
    composable(Routes.HEALTH_DASHBOARD) {
        HealthDashboardScreen(
            userEmail = userEmail,
            onNavigate = { route ->
                navController.navigate(route) { launchSingleTop = true }
            }
        )
    }
    composable(Routes.STEP_COUNTER) {
        StepCounterScreen(userEmail, onBack = { navController.popBackStack() })
    }
    composable(Routes.WATER_TRACKER) {
        WaterTrackerScreen(userEmail, onBack = { navController.popBackStack() })
    }
    composable(Routes.FOOD_SCANNER) {
        FoodScannerScreen(
            userEmail = userEmail,
            onBack = { navController.popBackStack() },
            onNavigateToCamera = { mealTitle ->
                navController.navigate(Routes.FOOD_CAMERA.replace("{mealTitle}", mealTitle))
            }
        )
    }
    composable(Routes.FOOD_CAMERA) { backStackEntry ->
        val mealTitle = backStackEntry.arguments?.getString("mealTitle") ?: "Meal"
        FoodCameraScreen(
            mealTitle = mealTitle,
            onBackClick = { navController.popBackStack() },
            onLogMeal = { items ->
                // MOCK: Handle saving meals
                navController.popBackStack()
            }
        )
    }
    composable(Routes.HEALTH_STATS) {
        HealthStatsScreen(userEmail, onBack = { navController.popBackStack() })
    }
    composable(Routes.HEALTH_PREDICTION) {
        HealthPredictionScreen(userEmail, onBack = { navController.popBackStack() })
    }
    composable(Routes.HEALTH_LOG) {
        HealthLogScreen(userEmail, onBack = { navController.popBackStack() })
    }
    composable(Routes.SLEEP_SCHEDULE) {
        SleepScheduleScreen(onBack = { navController.popBackStack() })
    }
}
