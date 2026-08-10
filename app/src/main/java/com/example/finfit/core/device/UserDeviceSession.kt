package com.example.finfit.core.device

data class UserDeviceSession(
    val deviceId: String,
    val deviceName: String,           // e.g. "Samsung Galaxy S24 Ultra"
    val modelName: String,            // e.g. "SM-S928B"
    val osVersion: String,            // e.g. "Android 14 (API 34)"
    val appVersion: String,           // e.g. "1.0.0"
    val lastActiveTimestamp: Long,
    val isCurrentDevice: Boolean = false,
    val ipAddressOrCity: String = "Việt Nam"
)
