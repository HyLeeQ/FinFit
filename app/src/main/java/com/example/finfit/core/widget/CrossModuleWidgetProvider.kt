package com.example.finfit.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.finfit.MainActivity
import com.example.finfit.R
import com.example.finfit.finance.model.TransactionType
import com.example.finfit.finance.repository.FirestoreRepository
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class CrossModuleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_cross_module)

            val now = Calendar.getInstance()
            val currentDay = now.get(Calendar.DAY_OF_YEAR)
            val currentYear = now.get(Calendar.YEAR)
            val tempCal = Calendar.getInstance()

            val txs = FirestoreRepository.cachedTransactions
            val todaySpent = txs.filter {
                (it.type == TransactionType.EXPENSE || it.type == TransactionType.GROUP_PREPAYMENT) &&
                run {
                    tempCal.time = it.timestamp.toDate()
                    tempCal.get(Calendar.DAY_OF_YEAR) == currentDay && tempCal.get(Calendar.YEAR) == currentYear
                }
            }.sumOf { if (it.isGroupPrepayment) it.personalAmount else it.amount }

            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            val spentStr = "${formatter.format(todaySpent)} đ"

            views.setTextViewText(R.id.tv_widget_today_spent, spentStr)
            views.setTextViewText(R.id.tv_widget_today_steps, "8.420")
            views.setTextViewText(R.id.tv_widget_today_calories, "1.850 kcal")
            views.setTextViewText(R.id.tv_widget_cross_streak, "🔥 Chuỗi 6 ngày")

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_SCREEN", "INSIGHTS")
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 201, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_cross_module_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
