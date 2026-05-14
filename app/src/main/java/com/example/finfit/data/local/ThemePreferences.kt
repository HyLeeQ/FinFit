package com.example.finfit.data.local

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setLastTab(route: String) {
        prefs.edit().putString("last_tab", route).apply()
    }

    fun getLastTab(): String {
        return prefs.getString("last_tab", com.example.finfit.core.navigation.Routes.DASHBOARD) ?: com.example.finfit.core.navigation.Routes.DASHBOARD
    }

    fun hasSeenOnboarding(): Boolean {
        return prefs.getBoolean("has_seen_onboarding", false)
    }

    fun setOnboardingSeen() {
        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
    }
}
