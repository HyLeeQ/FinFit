package com.example.finfit.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.finfit.MainActivity
import com.example.finfit.R

class QuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add)

            // Button 1: Ăn uống
            val foodIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("QUICK_ACTION", "ADD_TRANSACTION")
                putExtra("DEFAULT_CATEGORY", "Ăn uống")
            }
            views.setOnClickPendingIntent(
                R.id.btn_widget_add_food,
                PendingIntent.getActivity(context, 101, foodIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Button 2: Mua sắm
            val shopIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("QUICK_ACTION", "ADD_TRANSACTION")
                putExtra("DEFAULT_CATEGORY", "Mua sắm")
            }
            views.setOnClickPendingIntent(
                R.id.btn_widget_add_shopping,
                PendingIntent.getActivity(context, 102, shopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Button 3: Đi lại
            val transportIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("QUICK_ACTION", "ADD_TRANSACTION")
                putExtra("DEFAULT_CATEGORY", "Đi lại")
            }
            views.setOnClickPendingIntent(
                R.id.btn_widget_add_transport,
                PendingIntent.getActivity(context, 103, transportIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Button 4: Quét Bill OCR
            val scanIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("QUICK_ACTION", "SCAN_BILL")
            }
            views.setOnClickPendingIntent(
                R.id.btn_widget_scan_bill,
                PendingIntent.getActivity(context, 104, scanIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
