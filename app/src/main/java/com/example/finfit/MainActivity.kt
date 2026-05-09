package com.example.finfit

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.example.finfit.service.BankNotificationListener
import com.example.finfit.data.local.SetupPreferences
import com.example.finfit.ui.AuthScreen
import com.example.finfit.ui.MainScreen
import com.example.finfit.ui.OnboardingScreen
import com.example.finfit.ui.SetupCategoriesScreen
import com.example.finfit.ui.SetupCurrencyScreen
import com.example.finfit.ui.SplashScreen
import com.example.finfit.ui.theme.FinFitTheme

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePrefs = ThemePreferences(this)
        val setupPrefs = SetupPreferences(this)
        
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
                                val dest = when {
                                    !themePrefs.hasSeenOnboarding() -> Routes.ONBOARDING
                                    authRepository.getCurrentUser() == null -> Routes.AUTH
                                    else -> Routes.MAIN
                                }
                                navController.navigate(dest) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(
                            onFinished = {
                                // Go to currency setup (don't mark seen yet)
                                navController.navigate(Routes.SETUP_CURRENCY) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.SETUP_CURRENCY) {
                        SetupCurrencyScreen(
                            initialCurrency = setupPrefs.getCurrency(),
                            onBack = { navController.popBackStack() },
                            onCurrencySelected = { currency ->
                                setupPrefs.setCurrency(currency.code)
                                navController.navigate(Routes.SETUP_CATEGORIES) {
                                    popUpTo(Routes.SETUP_CURRENCY) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.SETUP_CATEGORIES) {
                        SetupCategoriesScreen(
                            onBack = { navController.popBackStack() },
                            onFinish = { expSel, incSel, custExp, custInc ->
                                setupPrefs.setEnabledExpenseCategories(expSel)
                                setupPrefs.setEnabledIncomeCategories(incSel)
                                setupPrefs.saveCustomExpenseCategories(custExp)
                                setupPrefs.saveCustomIncomeCategories(custInc)
                                themePrefs.setOnboardingSeen()
                                val dest = if (authRepository.getCurrentUser() == null) Routes.AUTH else Routes.MAIN
                                navController.navigate(dest) {
                                    popUpTo(Routes.SETUP_CATEGORIES) { inclusive = true }
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
    override fun onResume() {
        super.onResume()
        restartNotificationListenerIfNeeded()
    }

    /**
     * Kiểm tra xem user đã cấp quyền Notification Listener chưa.
     */
    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.contains(packageName)
    }

    /**
     * Nếu đã cấp quyền nhưng listener đang bị ngắt (thường do MIUI chặn AutoStart),
     * toggle disable → enable component để buộc hệ thống rebind lại.
     */
    private fun restartNotificationListenerIfNeeded() {
        if (!isNotificationListenerEnabled()) return  // Chưa cấp quyền → không làm gì
        if (BankNotificationListener.isConnected) return  // Đang hoạt động tốt → không cần

        Log.d("MainActivity", "Listener không kết nối, thử toggle lại...")
        val pm = packageManager
        val cn = ComponentName(this, BankNotificationListener::class.java)
        try {
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            Log.d("MainActivity", "Toggle listener thành công")
        } catch (e: Exception) {
            Log.e("MainActivity", "Lỗi toggle listener: ${e.message}")
        }
    }
}
