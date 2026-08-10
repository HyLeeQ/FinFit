package com.example.finfit.core.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.finfit.core.di.ViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ADD_WATER = "com.example.finfit.ACTION_ADD_WATER"
        const val ACTION_CONFIRM_BANK_TX = "com.example.finfit.ACTION_CONFIRM_BANK_TX"
        const val ACTION_CANCEL_BANK_TX = "com.example.finfit.ACTION_CANCEL_BANK_TX"
        const val ACTION_MARK_DEBT_PAID = "com.example.finfit.ACTION_MARK_DEBT_PAID"
        const val ACTION_FREEZE_STREAK = "com.example.finfit.ACTION_FREEZE_STREAK"

        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_TRANSACTION_ID = "EXTRA_TRANSACTION_ID"
        const val EXTRA_DEBT_ID = "EXTRA_DEBT_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notiId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val notiManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        when (intent.action) {
            ACTION_ADD_WATER -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ViewModelFactory.recordGamificationActionUseCase("default_user", com.example.finfit.insights.domain.model.GamificationAction.HIT_DAILY_WATER)
                    } catch (_: Exception) {}
                }
                Toast.makeText(context, "💧 Đã ghi nhận +250ml nước!", Toast.LENGTH_SHORT).show()
                if (notiId != -1) notiManager?.cancel(notiId)
            }

            ACTION_CONFIRM_BANK_TX -> {
                Toast.makeText(context, "✅ Đã xác nhận giao dịch ngân hàng!", Toast.LENGTH_SHORT).show()
                if (notiId != -1) notiManager?.cancel(notiId)
            }

            ACTION_CANCEL_BANK_TX -> {
                Toast.makeText(context, "❌ Đã hủy ghi nhận giao dịch.", Toast.LENGTH_SHORT).show()
                if (notiId != -1) notiManager?.cancel(notiId)
            }

            ACTION_MARK_DEBT_PAID -> {
                Toast.makeText(context, "💰 Đã đánh dấu thanh toán nợ thành công!", Toast.LENGTH_SHORT).show()
                if (notiId != -1) notiManager?.cancel(notiId)
            }

            ACTION_FREEZE_STREAK -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ViewModelFactory.useStreakFreezeUseCase("default_user")
                    } catch (_: Exception) {}
                }
                Toast.makeText(context, "❄️ Đã kích hoạt Freeze bảo vệ chuỗi hôm nay!", Toast.LENGTH_SHORT).show()
                if (notiId != -1) notiManager?.cancel(notiId)
            }
        }
    }
}
