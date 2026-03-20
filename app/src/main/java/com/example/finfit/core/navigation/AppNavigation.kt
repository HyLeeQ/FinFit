package com.example.finfit.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
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
    const val ASSISTANT = "assistant" // Màn hình trợ lý AI mới
    // Sub-routes for Health
    const val HEALTH_DASHBOARD = "health_dashboard"
    const val STEP_COUNTER = "stepCounter" // Phân hệ đếm bước mới bằng Sensor/Room
    const val FOOD_SCANNER = "food_scanner"
    const val HEALTH_STATS = "health_stats"
    const val HEALTH_PREDICTION = "health_prediction"
    const val HEALTH_LOG = "health_log"
}

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Routes.DASHBOARD, Icons.Default.Home, "Trang chủ")
    object Assistant : BottomNavItem(Routes.ASSISTANT, Icons.Default.SmartToy, "Trợ lý AI")
    object Add : BottomNavItem(Routes.ADD, Icons.Default.Add, "Thêm")
    object Health : BottomNavItem(Routes.HEALTH_DASHBOARD, Icons.Default.Favorite, "Sức khỏe")
    object Profile : BottomNavItem(Routes.PROFILE, Icons.Default.Person, "Cá nhân")
}
