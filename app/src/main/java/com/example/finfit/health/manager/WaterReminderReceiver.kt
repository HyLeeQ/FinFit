package com.example.finfit.health.manager

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.finfit.MainActivity
import com.example.finfit.R
import com.example.finfit.health.repository.HealthRepository
import java.util.Calendar

class WaterReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val healthRepository = HealthRepository(context)
        
        // 1. Kiểm tra cấu hình có bật không
        if (!healthRepository.isWaterReminderEnabled()) {
            return
        }

        // 2. Kiểm tra có đang nằm trong giờ thức (Active Hours) không
        val bedTimeMinute = healthRepository.getBedTimeMinute()
        val wakeTimeMinute = healthRepository.getWakeTimeMinute()
        
        val cal = Calendar.getInstance()
        val currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        
        val isAwake = if (wakeTimeMinute <= bedTimeMinute) {
            // Giấc ngủ bình thường (vd: thức 08:00, ngủ 22:00)
            currentMinuteOfDay in wakeTimeMinute..bedTimeMinute
        } else {
            // Giấc ngủ qua đêm (vd: thức 06:00, ngủ 23:00 -> bed=1380, wake=360)
            currentMinuteOfDay >= wakeTimeMinute || currentMinuteOfDay <= bedTimeMinute
        }

        if (isAwake) {
            showNotification(context)
            // Schedule lại cho 2 tiếng sau
            WaterReminderManager.scheduleReminder(context)
        } else {
            // Đang trong giờ ngủ. Không làm phiền.
            // Sẽ cần một cơ chế để kích hoạt lại alarm vào lúc ngủ dậy (wakeTime)
            // Tạm thời có thể schedule lại báo thức vào đúng giờ dậy
            scheduleForWakeTime(context, wakeTimeMinute)
        }
    }

    private fun showNotification(context: Context) {
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, WaterReminderManager.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Thay bằng R.drawable.ic_water_drop nếu có
            .setContentTitle("Đến giờ uống nước rồi! 💧")
            .setContentText("Cơ thể bạn cần được cấp nước. Hãy uống một ly nước nhé!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(2001, builder.build())
            }
        } catch (e: SecurityException) {
            // Không có quyền gửi notification (Android 13+)
        }
    }
    
    private fun scheduleForWakeTime(context: Context, wakeTimeMinute: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, wakeTimeMinute / 60)
            set(Calendar.MINUTE, wakeTimeMinute % 60)
            set(Calendar.SECOND, 0)
        }
        
        // Nếu giờ dậy đã qua trong ngày hôm nay, cộng thêm 1 ngày
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        } catch (e: SecurityException) {
            // Fallback
        }
    }
}
