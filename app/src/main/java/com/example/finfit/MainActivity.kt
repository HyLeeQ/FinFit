package com.example.finfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finfit.core.navigation.Routes
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.data.model.UserWallet
import com.example.finfit.data.repository.FirestoreRepository
import com.example.finfit.ui.DashboardScreen
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
                                userEmail = user.email ?: "User",
                                firestoreRepository = firestoreRepository,
                                onLogout = {
                                    authRepository.signOut()
                                    navController.navigate(Routes.AUTH) {
                                        popUpTo(Routes.MAIN) { inclusive = true }
                                    }
                                }
                            )
                        } else {
                            navController.navigate(Routes.AUTH) {
                                popUpTo(0)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardWithData(
    userEmail: String,
    firestoreRepository: FirestoreRepository,
    onLogout: () -> Unit
) {
    val user = AuthRepository().getCurrentUser()
    var wallet by remember { mutableStateOf<UserWallet?>(null) }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            val data = firestoreRepository.getUserWallet(user.uid)
            if (data == null) {
                val newWallet = UserWallet(uid = user.uid)
                firestoreRepository.saveUserWallet(newWallet)
                wallet = newWallet
            } else {
                wallet = data
            }
        }
    }

    DashboardScreen(
        userEmail = userEmail,
        wallet = wallet,
        onLogout = onLogout
    )
}
