package com.example.finfit.health.repository

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.finfit.MainActivity
import com.example.finfit.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class StepCounterService : Service() {

    private var stepCounterManager: StepCounterManager? = null

    companion object {
        const val CHANNEL_ID = "finfit_step_counter_channel"
        const val NOTIFICATION_ID = 1001

        // Dùng để bật/tắt Service từ Activity/Screen
        fun start(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepCounterService::class.java))
        }

        // Kiểm tra Service đang sống hay không
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (StepCounterService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        stepCounterManager = StepCounterManager.getInstance(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Hiển thị Notification bắt buộc cho Foreground Service
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Bắt đầu lắng nghe Sensor + Google AI
        stepCounterManager?.startListening()

        // Nếu hệ thống kill service, tự khởi động lại
        return START_STICKY
    }

    override fun onDestroy() {
        stepCounterManager?.stopListening()
        stepCounterManager = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        // Silent Sync: Đẩy dữ liệu lần cuối khi user vuốt kill app
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            stepCounterManager?.flushToDatabase()
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.finfit.health.data.HealthSyncWorker>()
                .setConstraints(constraints)
                .build()
            
            androidx.work.WorkManager.getInstance(applicationContext).enqueue(syncRequest)
            // BỎ stopSelf() để Service tiếp tục chạy ngầm đếm bước chân
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Tạo Notification Channel (Bắt buộc từ Android 8.0+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Theo dõi bước chân",
                NotificationManager.IMPORTANCE_LOW // Không có tiếng, không rung
            ).apply {
                description = "Thông báo khi FinFit đang theo dõi bước chân"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        // Ấn vào Notification sẽ mở lại app
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingTapIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FinFit")
            .setContentText("Đang theo dõi sức khỏe của bạn")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)        // Không vuốt tắt được
            .setSilent(true)         // Không có tiếng
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingTapIntent)
            .build()
    }

    // Cho phép UI truy cập StepCounterManager để đọc StateFlow
    fun getManager(): StepCounterManager? = stepCounterManager
}
