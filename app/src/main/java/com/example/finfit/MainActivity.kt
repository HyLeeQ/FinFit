package com.example.finfit

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finfit.core.navigation.Routes
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.ui.DashboardWithData
import com.example.finfit.finance.ui.AddTransactionWithData
import com.example.finfit.ui.AuthScreen
import com.example.finfit.ui.MainScreen
import com.example.finfit.ui.theme.FinFitTheme

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinFitTheme {
                val navController = rememberNavController()
                val currentUser = authRepository.getCurrentUser()
                val startDestination = if (currentUser == null) Routes.AUTH else Routes.MAIN

                // Move refreshTrigger up to be shared across screens
                var refreshTrigger by remember { mutableStateOf(0) }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.AUTH) {
                        AuthScreen(
                            authRepository = authRepository,
                            onLoginSuccess = {
                                navController.navigate(Routes.MAIN) {
                                    popUpTo(Routes.AUTH) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.MAIN) {
                        val user = authRepository.getCurrentUser()
                        if (user != null) {
                            MainScreen(
                                userEmail = user.email ?: "",
                                firestoreRepository = firestoreRepository,
                                refreshTrigger = refreshTrigger,
                                onTransactionSaved = { refreshTrigger++ },
                                onLogout = {
                                    authRepository.signOut()
                                    navController.navigate(Routes.AUTH) {
                                        popUpTo(Routes.MAIN) { inclusive = true }
                                    }
                                },
                                onAction = { actionType ->
                                    val route = if (actionType != null) "add?type=${actionType.name}" else Routes.ADD
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                    composable(
                        Routes.ADD + "?type={type}",
                        arguments = listOf(
                            androidx.navigation.navArgument("type") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val typeArg = backStackEntry.arguments?.getString("type")
                        AddTransactionWithData(
                            firestoreRepository = firestoreRepository,
                            onTransactionSaved = { 
                                refreshTrigger++ 
                                navController.popBackStack() 
                            },
                            onBack = { navController.popBackStack() },
                            initialTypeArg = typeArg
                        )
                    }
                }
            }
        }
    }
}
