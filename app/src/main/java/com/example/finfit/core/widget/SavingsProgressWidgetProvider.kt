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

class SavingsProgressWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_savings_progress)

            val goals = FirestoreRepository.cachedGoals
            val priorityGoal = goals.firstOrNull { it.currentAmount < it.targetAmount } ?: goals.firstOrNull()

            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

            if (priorityGoal != null) {
                val current = priorityGoal.currentAmount
                val target = priorityGoal.targetAmount.coerceAtLeast(1.0)
                val percent = ((current / target) * 100).toInt().coerceIn(0, 100)

                views.setTextViewText(R.id.tv_widget_goal_name, priorityGoal.goalName)
                views.setTextViewText(R.id.tv_widget_goal_amounts, "${formatter.format(current)} đ / ${formatter.format(target)} đ")
                views.setProgressBar(R.id.pb_widget_goal_progress, 100, percent, false)
                views.setTextViewText(R.id.tv_widget_goal_percent, "$percent% hoàn thành")
            } else {
                views.setTextViewText(R.id.tv_widget_goal_name, "Quỹ Khẩn Cấp")
                views.setTextViewText(R.id.tv_widget_goal_amounts, "Chưa có mục tiêu")
                views.setProgressBar(R.id.pb_widget_goal_progress, 100, 0, false)
                views.setTextViewText(R.id.tv_widget_goal_percent, "0% hoàn thành")
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_SCREEN", "SAVINGS")
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_savings_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
