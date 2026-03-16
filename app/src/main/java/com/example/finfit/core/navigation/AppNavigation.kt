package com.example.finfit.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// Định nghĩa các tên màn hình
object Routes {
    const val AUTH = "auth"
    const val MAIN = "main"
    const val DASHBOARD = "dashboard"
    const val FINANCE = "finance"
    const val HEALTH = "health"
    const val ADD = "add"
    const val PROFILE = "profile"
    // Sub-routes for Health
    const val HEALTH_DASHBOARD = "health_dashboard"
    const val STEP_COUNTER = "stepCounter" // Phân hệ đếm bước mới bằng Sensor/Room
    const val STEP_TRACKER = "step_tracker" // (Cũ - Placeholder)
    const val FOOD_SCANNER = "food_scanner"
    const val HEALTH_STATS = "health_stats"
    const val HEALTH_PREDICTION = "health_prediction"
    const val HEALTH_LOG = "health_log"
}

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Routes.DASHBOARD, Icons.Default.Home, "Trang chủ")
    object Finance : BottomNavItem(Routes.FINANCE, Icons.Default.Payments, "Tài chính")
    object Add : BottomNavItem(Routes.ADD, Icons.Default.Add, "Thêm")
    object Health : BottomNavItem(Routes.HEALTH_DASHBOARD, Icons.Default.Favorite, "Sức khỏe")
    object Profile : BottomNavItem(Routes.PROFILE, Icons.Default.Settings, "Cá nhân")
}
