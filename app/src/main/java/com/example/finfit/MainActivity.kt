package com.example.finfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.finfit.core.navigation.Routes
import com.example.finfit.data.local.ThemeMode
import com.example.finfit.data.local.ThemePreferences
import com.example.finfit.data.repository.AuthRepository
import com.example.finfit.finance.repository.FirestoreRepository
import com.example.finfit.finance.ui.wrappers.AddTransactionWithData
import com.example.finfit.ui.AuthScreen
import com.example.finfit.ui.MainScreen
import com.example.finfit.ui.SplashScreen
import com.example.finfit.ui.theme.FinFitTheme

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePrefs = ThemePreferences(this)
        
        setContent {
            var themeMode by remember { mutableStateOf(themePrefs.getThemeMode()) }
            val useDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            FinFitTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val currentUser = authRepository.getCurrentUser()
                
                var refreshTrigger by remember { mutableStateOf(0) }

                NavHost(navController = navController, startDestination = Routes.SPLASH) {
                    composable(Routes.SPLASH) {
                        SplashScreen(
                            onSplashFinished = {
                                val dest = if (authRepository.getCurrentUser() == null) Routes.AUTH else Routes.MAIN
                                navController.navigate(dest) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }
                        )
                    }
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
                                themeMode = themeMode,
                                onThemeChange = { newMode ->
                                    themeMode = newMode
                                    themePrefs.setThemeMode(newMode)
                                },
                                initialTab = themePrefs.getLastTab(),
                                onTabSelected = { tab -> themePrefs.setLastTab(tab) }
                            )
                        }
                    }
                }
            }
        }
    }
}
