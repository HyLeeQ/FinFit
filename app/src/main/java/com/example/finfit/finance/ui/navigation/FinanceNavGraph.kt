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
        // Placeholder for future feature
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Text("Tính năng Kế hoạch chi tiêu sẽ phát triển sau", color = MaterialTheme.colorScheme.onBackground)
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
}
