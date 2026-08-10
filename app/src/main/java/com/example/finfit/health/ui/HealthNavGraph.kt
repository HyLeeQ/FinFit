package com.example.finfit.health.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.finfit.core.navigation.Routes

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

fun NavGraphBuilder.healthNavGraph(
    navController: NavHostController,
    userEmail: String,
    firestoreRepository: com.example.finfit.finance.repository.FirestoreRepository
) {
    composable(Routes.HEALTH_DASHBOARD) {
        val user = remember { com.example.finfit.data.repository.AuthRepository().getCurrentUser() }
        val walletState = if (user != null) {
            firestoreRepository.observeUserWallet(user.uid).collectAsState(initial = null)
        } else {
            remember { mutableStateOf(null) }
        }
        val transactionsState = if (user != null) {
            firestoreRepository.observeTransactions(user.uid).collectAsState(initial = emptyList())
        } else {
            remember { mutableStateOf(emptyList()) }
        }
        val goalsState = if (user != null) {
            firestoreRepository.observeSavingsGoals(user.uid).collectAsState(initial = emptyList())
        } else {
            remember { mutableStateOf(emptyList()) }
        }

        HealthDashboardScreen(
            userEmail = userEmail,
            onNavigate = { route ->
                navController.navigate(route) { launchSingleTop = true }
            },
            wallet = walletState.value,
            transactions = transactionsState.value,
            goals = goalsState.value
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
            onLogMeal = { _ ->
                // Safe navigation: only pop if this destination is still on the stack
                if (navController.currentDestination?.route?.startsWith("food_camera") == true) {
                    navController.popBackStack()
                }
            }
        )
    }
    composable(Routes.HEALTH_NEWS) {
        NewsScreen(onNavigateToDetail = { articleId -> 
            navController.navigate(Routes.HEALTH_NEWS_DETAIL.replace("{articleId}", articleId)) 
        })
    }
    composable(Routes.HEALTH_NEWS_DETAIL) { backStackEntry ->
        val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
        NewsDetailScreen(articleId = articleId, onBack = { navController.popBackStack() })
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
