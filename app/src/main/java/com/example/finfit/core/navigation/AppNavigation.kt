package com.example.finfit.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

// Định nghĩa các tên màn hình
object Routes {
    const val AUTH = "auth"
    const val MAIN = "main"
    const val ADD = "add"
    // Finance Routes
    const val DASHBOARD = "dashboard" // Trang chủ tài chính
    const val FINANCE_WALLET = "finance_wallet" // Quản lý ví
    const val FINANCE_PLAN = "finance_plan" // Kế hoạch chi tiêu
    const val SAVINGS_GOALS = "savings_goals"
    const val HELD_FUNDS = "held_funds"
    const val GENERAL_SAVINGS = "general_savings"
    const val TRANSFER = "transfer"
    const val BUDGET = "budget"
    const val ANALYTICS = "analytics"
    const val DEBT_LOAN = "debt_loan"
    const val TRANSACTION_HISTORY = "transaction_history"
    const val PROFILE = "profile" // Cá nhân chung
    const val ASSISTANT = "assistant"

    // Health Routes
    const val HEALTH_DASHBOARD = "health_dashboard" // Y tế cơ bản
    const val WATER_TRACKER = "water_tracker"
    const val STEP_COUNTER = "stepCounter"
    const val FOOD_SCANNER = "food_scanner"
    const val HEALTH_STATS = "health_stats"
    const val HEALTH_PREDICTION = "health_prediction"
    const val HEALTH_LOG = "health_log"
}

enum class AppMode {
    FINANCE, HEALTH
}

// Bottom Navigation items based on mode
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    // Finance
    object FinanceHome : BottomNavItem(Routes.DASHBOARD, Icons.Default.Home, "Trang chủ")
    object FinanceWallet : BottomNavItem(Routes.FINANCE_WALLET, Icons.Default.AccountBalanceWallet, "Quản lý ví")
    object FinancePlan : BottomNavItem(Routes.FINANCE_PLAN, Icons.AutoMirrored.Filled.List, "Sắp xếp")
    
    // Health 
    object HealthHome : BottomNavItem(Routes.HEALTH_DASHBOARD, Icons.Default.Favorite, "Sức khỏe")
    object HealthFeatures : BottomNavItem(Routes.HEALTH_STATS, Icons.Default.BarChart, "Phân tích")
    
    // Common
    object Profile : BottomNavItem(Routes.PROFILE, Icons.Default.Person, "Cá nhân")
}
