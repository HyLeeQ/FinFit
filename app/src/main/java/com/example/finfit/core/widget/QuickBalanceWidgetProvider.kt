package com.example.finfit.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.finfit.MainActivity
import com.example.finfit.R
import com.example.finfit.finance.repository.FirestoreRepository
import java.text.NumberFormat
import java.util.Locale

class QuickBalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_balance)

            // Get cached wallet or default
            val wallet = FirestoreRepository.cachedWallet
            val totalBalance = wallet?.totalBalance ?: 0.0
            val spendable = (totalBalance - (wallet?.generalSavings ?: 0.0)).coerceAtLeast(0.0)

            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            val totalStr = "${formatter.format(totalBalance)} đ"
            val spendableStr = "Khả dụng: ${formatter.format(spendable)} đ"

            views.setTextViewText(R.id.tv_widget_total_balance, totalStr)
            views.setTextViewText(R.id.tv_widget_spendable_balance, spendableStr)

            // Tap on widget opens MainActivity / Dashboard
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_balance_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
