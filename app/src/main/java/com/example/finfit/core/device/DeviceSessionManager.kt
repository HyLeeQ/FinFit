package com.example.finfit.core.device

import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object DeviceSessionManager {

    val currentDeviceId: String by lazy {
        UUID.randomUUID().toString().take(12)
    }

    private val _activeSessions = MutableStateFlow<List<UserDeviceSession>>(
        listOf(
            UserDeviceSession(
                deviceId = currentDeviceId,
                deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                modelName = Build.DEVICE,
                osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                appVersion = "FinFit v1.0",
                lastActiveTimestamp = System.currentTimeMillis(),
                isCurrentDevice = true,
                ipAddressOrCity = "TP. Hồ Chí Minh, VN"
            ),
            UserDeviceSession(
                deviceId = "dev_ipad_tablet",
                deviceName = "Xiaomi Pad 6 Pro",
                modelName = "liuqin",
                osVersion = "Android 14 (HyperOS)",
                appVersion = "FinFit v1.0",
                lastActiveTimestamp = System.currentTimeMillis() - 86400000L, // 1 ngày trước
                isCurrentDevice = false,
                ipAddressOrCity = "Hà Nội, VN"
            )
        )
    )

    fun observeActiveDevices(): Flow<List<UserDeviceSession>> = _activeSessions.asStateFlow()

    fun revokeDeviceSession(deviceId: String): Boolean {
        if (deviceId == currentDeviceId) return false // Cannot revoke current device directly
        _activeSessions.value = _activeSessions.value.filter { it.deviceId != deviceId }
        return true
    }
}
