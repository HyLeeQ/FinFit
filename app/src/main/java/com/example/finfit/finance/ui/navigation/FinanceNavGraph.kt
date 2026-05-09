package com.example.finfit.finance.ui.navigation

import com.example.finfit.finance.ui.screens.*
import com.example.finfit.finance.ui.wrappers.*

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import com.example.finfit.core.navigation.Routes
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.repository.FirestoreRepository

fun NavGraphBuilder.financeNavGraph(
    navController: NavHostController,
    userEmail: String,
    firestoreRepository: FirestoreRepository,
    refreshTrigger: Int,
    onLogout: () -> Unit,
    onAction: (TransactionType?) -> Unit
) {
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
        val user = com.example.finfit.data.repository.AuthRepository().getCurrentUser()
        if (user != null) {
            WeeklyScheduleWrapper(
                uid = user.uid,
                firestoreRepository = firestoreRepository,
                onBack = { navController.popBackStack() }
            )
        }
    }
    composable(Routes.SAVINGS_GOALS) {
        SavingsGoalWithData(
            firestoreRepository = firestoreRepository,
            onBack = { navController.popBackStack() }
        )
    }
    composable(Routes.HELD_FUNDS) {
        HeldFundsWithData(
            firestoreRepository = firestoreRepository,
            onBack = { navController.popBackStack() }
        )
    }
    composable(Routes.GENERAL_SAVINGS) {
        GeneralSavingsWithData(
            firestoreRepository = firestoreRepository,
            onBack = { navController.popBackStack() }
        )
    }
    composable(Routes.TRANSFER) {
        InternalTransferWithDataFixed(
            firestoreRepository = firestoreRepository,
            onBack = { navController.popBackStack() }
        )
    }
    composable(Routes.BUDGET) {
        val user = com.example.finfit.data.repository.AuthRepository().getCurrentUser()
        if (user != null) {
            BudgetWrapper(uid = user.uid, onNavigateBack = { navController.popBackStack() })
        }
    }
    composable(Routes.ANALYTICS) {
        val user = com.example.finfit.data.repository.AuthRepository().getCurrentUser()
        if (user != null) {
            AnalyticsWrapper(uid = user.uid, onBack = { navController.popBackStack() })
        }
    }
    composable(Routes.DEBT_LOAN) {
        val user = com.example.finfit.data.repository.AuthRepository().getCurrentUser()
        if (user != null) {
            DebtLoanWrapper(uid = user.uid, onBack = { navController.popBackStack() })
        }
    }
    composable(Routes.TRANSACTION_HISTORY) {
        TransactionHistoryWithData(
            firestoreRepository = firestoreRepository,
            onBack = { navController.popBackStack() }
        )
    }
    composable(
        Routes.ADD + "?type={type}&camera={camera}",
        arguments = listOf(
            navArgument("type") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("camera") {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { backStackEntry ->
        val typeArg = backStackEntry.arguments?.getString("type")
        val cameraArg = backStackEntry.arguments?.getBoolean("camera") ?: false
        AddTransactionWithData(
            firestoreRepository = firestoreRepository,
            onTransactionSaved = { 
                // refreshTrigger handle refresh
                navController.popBackStack() 
            },
            onBack = { navController.popBackStack() },
            onHome = { navController.popBackStack() },
            initialTypeArg = typeArg,
            autoOpenCamera = cameraArg
        )
    }
    composable(Routes.PHOTO_DIARY) {
        val user = com.example.finfit.data.repository.AuthRepository().getCurrentUser()
        if (user != null) {
            PhotoDiaryWithData(
                firestoreRepository = firestoreRepository,
                onBack = { navController.popBackStack() },
                onNavigateToAddTransaction = { navController.navigate(Routes.ADD) }
            )
        }
    }
}
