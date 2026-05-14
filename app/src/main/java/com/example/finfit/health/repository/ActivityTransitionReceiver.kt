package com.example.finfit.health.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            result?.let {
                val mostProbableActivity = it.mostProbableActivity
                val activityType = mostProbableActivity.type
                val confidence = mostProbableActivity.confidence

                val prefs = context.getSharedPreferences("StepTrackerPrefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()

                when (activityType) {
                    // CHỈ khoá bước khi CHẮC CHẮN đang trên xe (confidence > 75%)
                    DetectedActivity.IN_VEHICLE -> {
                        if (confidence > 75) {
                            editor.putBoolean("isUserWalking", false)
                            editor.putLong("lastActivityUpdateTime", System.currentTimeMillis())
                        }
                    }
                    // Đi bộ, chạy bộ → mở khoá bước ngay (confidence > 30% là đủ)
                    DetectedActivity.WALKING,
                    DetectedActivity.RUNNING,
                    DetectedActivity.ON_FOOT -> {
                        if (confidence > 30) {
                            editor.putBoolean("isUserWalking", true)
                            editor.putLong("lastActivityUpdateTime", System.currentTimeMillis())
                        }
                    }
                    // STILL: KHÔNG khoá bước — vì giữa mỗi bước đi đều có khoảnh khắc "đứng yên"
                    // KHÔNG ghi timestamp để AI timeout (60s) có thể tự động gỡ khoá nếu vừa xuống xe
                    DetectedActivity.STILL -> {
                        // Không làm gì cả để cơ chế Timeout 60s phát huy tác dụng
                    }
                    else -> {
                        // Trạng thái khác (ON_BICYCLE, TILTING, UNKNOWN): giữ nguyên
                    }
                }
                editor.apply()
            }
        }
    }
}
