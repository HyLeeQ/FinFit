package com.example.finfit.core.security

import android.app.Activity
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PrivacyModeManager {

    private val _isPrivacyModeActive = MutableStateFlow(false)
    val isPrivacyModeActive: StateFlow<Boolean> = _isPrivacyModeActive.asStateFlow()

    private val _isScreenSecureEnabled = MutableStateFlow(true)
    val isScreenSecureEnabled: StateFlow<Boolean> = _isScreenSecureEnabled.asStateFlow()

    fun togglePrivacyMode() {
        _isPrivacyModeActive.value = !_isPrivacyModeActive.value
    }

    fun setPrivacyMode(active: Boolean) {
        _isPrivacyModeActive.value = active
    }

    fun setScreenSecureEnabled(activity: Activity, enabled: Boolean) {
        _isScreenSecureEnabled.value = enabled
        if (enabled) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun maskAmount(amountStr: String): String {
        return if (_isPrivacyModeActive.value) "•••••• đ" else amountStr
    }
}
