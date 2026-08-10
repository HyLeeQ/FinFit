package com.example.finfit.core.security

import android.util.Log

object SecurityAuditLogger {

    private const val TAG = "FinFitSecurityAudit"

    enum class SecurityEvent(val eventName: String) {
        APP_UNLOCKED("Mở khóa ứng dụng thành công"),
        APP_LOCK_FAILED("Xác thực sinh trắc học thất bại"),
        REMOTE_DEVICE_REVOKED("Thu hồi phiên thiết bị từ xa"),
        EXPORT_DATA_TRIGGERED("Khởi chạy xuất dữ liệu cá nhân"),
        LARGE_TRANSACTION_WARNED("Cảnh báo giao dịch giá trị lớn"),
        ACCOUNT_DELETE_REQUESTED("Yêu cầu xóa toàn bộ dữ liệu tài khoản")
    }

    fun logEvent(event: SecurityEvent, details: String = "") {
        val sanitizedDetails = DataAnonymizer.sanitizeRawText(details)
        Log.i(TAG, "🛡️ [AUDIT] ${event.eventName}: $sanitizedDetails")
    }
}
