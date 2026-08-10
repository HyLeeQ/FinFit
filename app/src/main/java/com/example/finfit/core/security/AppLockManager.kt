package com.example.finfit.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLockMode(val title: String, val description: String) {
    ALWAYS("Mỗi lần mở app", "Yêu cầu vân tay / Face ID ngay khi mở app"),
    TIMEOUT_5MIN("Sau 5 phút", "Khóa sau 5 phút không hoạt động"),
    TIMEOUT_15MIN("Sau 15 phút", "Khóa sau 15 phút không hoạt động"),
    DISABLED("Không khóa", "Mở app trực tiếp không cần xác thực")
}

object AppLockManager {

    private val _lockMode = MutableStateFlow(AppLockMode.ALWAYS)
    val lockMode: StateFlow<AppLockMode> = _lockMode.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var lastActivityTimestamp: Long = System.currentTimeMillis()

    fun setLockMode(mode: AppLockMode) {
        _lockMode.value = mode
        if (mode == AppLockMode.DISABLED) {
            _isAppLocked.value = false
        }
    }

    fun recordUserActivity() {
        lastActivityTimestamp = System.currentTimeMillis()
    }

    fun checkAndApplyLockOnResume() {
        if (_lockMode.value == AppLockMode.DISABLED) return

        val now = System.currentTimeMillis()
        val elapsed = now - lastActivityTimestamp

        when (_lockMode.value) {
            AppLockMode.ALWAYS -> {
                _isAppLocked.value = true
            }
            AppLockMode.TIMEOUT_5MIN -> {
                if (elapsed > 5 * 60 * 1000L) {
                    _isAppLocked.value = true
                }
            }
            AppLockMode.TIMEOUT_15MIN -> {
                if (elapsed > 15 * 60 * 1000L) {
                    _isAppLocked.value = true
                }
            }
            AppLockMode.DISABLED -> {}
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
        recordUserActivity()
    }

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        title: String = "Xác thực bảo mật FinFit",
        subtitle: String = "Vui lòng quét vân tay hoặc Face ID",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                unlockApp()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Xác thực không khớp, vui lòng thử lại.")
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }
}
