package com.example.finfit.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.finfit.MainActivity
import com.example.finfit.R
import com.example.finfit.finance.model.FinanceBudget
import com.example.finfit.finance.model.FinanceTransaction
import com.example.finfit.finance.ui.logic.BudgetLogic
import java.text.NumberFormat
import java.util.Locale

object SmartNotificationManager {

    private const val CHANNEL_PREDICTIVE_BUDGET = "channel_predictive_budget"
    private const val CHANNEL_ACTIONABLE = "channel_actionable"
    private const val CHANNEL_EVENING_DIGEST = "channel_evening_digest"
    private const val CHANNEL_GAMIFICATION = "channel_gamification"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val chBudget = NotificationChannel(
                CHANNEL_PREDICTIVE_BUDGET,
                "Cảnh Báo Ngân Sách Dự Đoán",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Cảnh báo sớm tốc độ chi tiêu trước khi cạn ngân sách" }

            val chActionable = NotificationChannel(
                CHANNEL_ACTIONABLE,
                "Thông Báo Tương Tác Nhanh",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Cho phép bấm nút ghi nhận giao dịch, uống nước, trả nợ trực tiếp" }

            val chDigest = NotificationChannel(
                CHANNEL_EVENING_DIGEST,
                "Bản Tin Tổng Hợp Buổi Tối",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Tóm tắt chi tiêu & sức khỏe cuối ngày (20:30)" }

            val chGamification = NotificationChannel(
                CHANNEL_GAMIFICATION,
                "Huy Hiệu & Động Lực",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Khen ngợi thành tích, chuỗi streak và lên cấp" }

            manager.createNotificationChannels(listOf(chBudget, chActionable, chDigest, chGamification))
        }
    }

    /** 1. Cảnh báo dự đoán ngân sách (Predictive Budget Alert) */
    fun checkAndNotifyPredictiveBudget(context: Context, budget: FinanceBudget, txs: List<FinanceTransaction>) {
        val pace = BudgetLogic.calculateSpendingPace(budget, txs)
        if (pace.isProjectedToOverspend) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_SCREEN", "BUDGET")
            }
            val pi = PendingIntent.getActivity(context, 301, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            val projectedStr = "${formatter.format(pace.projectedMonthEndSpent)} đ"

            val noti = NotificationCompat.Builder(context, CHANNEL_PREDICTIVE_BUDGET)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⚠️ Dự Báo Ngân Sách: ${budget.category}")
                .setContentText("Với tốc độ hiện tại, dự kiến cuối tháng sẽ chi $projectedStr (vượt hạn mức).")
                .setStyle(NotificationCompat.BigTextStyle().bigText("⚠️ **Dự báo ngân sách ${budget.category}**:\nVới tốc độ chi tiêu hiện tại, dự kiến cuối tháng bạn sẽ tiêu $projectedStr. ${pace.paceSummary}"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()

            manager.notify(budget.category.hashCode(), noti)
        }
    }

    /** 2. Thông báo giao dịch ngân hàng kèm nút bấm hành động (Actionable SMS/Bank Notification) */
    fun showActionableBankTransactionNotification(
        context: Context,
        bankCode: String,
        amount: Double,
        note: String,
        txId: String
    ) {
        val notiId = (System.currentTimeMillis() % 10000).toInt()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val amountStr = "${formatter.format(amount)} đ"

        // Action 1: Xác nhận
        val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CONFIRM_BANK_TX
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notiId)
            putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, txId)
        }
        val confirmPi = PendingIntent.getBroadcast(context, notiId * 10 + 1, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Action 2: Hủy bỏ
        val cancelIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CANCEL_BANK_TX
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notiId)
            putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, txId)
        }
        val cancelPi = PendingIntent.getBroadcast(context, notiId * 10 + 2, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val noti = NotificationCompat.Builder(context, CHANNEL_ACTIONABLE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📲 [$bankCode] Giao dịch mới: $amountStr")
            .setContentText(note.take(80))
            .setStyle(NotificationCompat.BigTextStyle().bigText("FinFit đã tự động ghi nhận giao dịch $amountStr từ $bankCode.\nNội dung: $note"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.mipmap.ic_launcher, "✅ Xác Nhận", confirmPi)
            .addAction(R.mipmap.ic_launcher, "❌ Hủy Bỏ", cancelPi)
            .setAutoCancel(true)
            .build()

        manager.notify(notiId, noti)
    }

    /** 3. Thông báo nhắc uống nước với nút bấm +250ml trực tiếp */
    fun showActionableWaterReminder(context: Context) {
        val notiId = 9991
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val addWaterIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ADD_WATER
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notiId)
        }
        val addWaterPi = PendingIntent.getBroadcast(context, 401, addWaterIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val noti = NotificationCompat.Builder(context, CHANNEL_ACTIONABLE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💧 Đã đến giờ uống nước rồi!")
            .setContentText("Uống 1 ly nước để giữ cơ thể sảng khoái và duy trì năng lượng nhé.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.mipmap.ic_launcher, "+250ml Nước", addWaterPi)
            .setAutoCancel(true)
            .build()

        manager.notify(notiId, noti)
    }

    /** 4. Bản tin tổng hợp chéo module buổi tối (Cross-Module Evening Digest) */
    fun showCrossModuleEveningDigest(
        context: Context,
        spentToday: Double,
        stepsToday: Int,
        caloriesToday: Int,
        streakDays: Int
    ) {
        val notiId = 8888
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        val spentStr = "${formatter.format(spentToday)} đ"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("OPEN_SCREEN", "INSIGHTS")
        }
        val openPi = PendingIntent.getActivity(context, 501, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val noti = NotificationCompat.Builder(context, CHANNEL_EVENING_DIGEST)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🌙 Bản Tin Tổng Kết FinFit Hôm Nay")
            .setContentText("Chi tiêu: $spentStr • Vận động: $stepsToday bước • Chuỗi: $streakDays ngày 🔥")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                """
                🌙 **Tổng kết một ngày tuyệt vời của bạn:**
                • 💰 Chi tiêu hôm nay: $spentStr
                • 🏃‍♂️ Bước chân: $stepsToday bước
                • 🥗 Năng lượng nạp: $caloriesToday kcal
                • 🔥 Chuỗi Streak: $streakDays ngày liên tiếp!
                
                Làm rất tốt! Chúc bạn ngủ ngon và tái tạo năng lượng cho ngày mai.
                """.trimIndent()
            ))
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .build()

        manager.notify(notiId, noti)
    }
}
