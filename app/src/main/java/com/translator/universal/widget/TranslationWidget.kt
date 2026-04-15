package com.translator.universal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.translator.universal.R
import com.translator.universal.ui.main.MainActivity

class TranslationWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Widget enabled for first time
    }

    override fun onDisabled(context: Context) {
        // Last widget instance removed
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_translation).apply {
                setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)
                setTextViewText(R.id.widgetTitle, "Evrensel Çevirmen")
                setTextViewText(R.id.widgetSubtitle, "Hızlı çeviri için dokunun")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class QuickTranslateWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateQuickWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateQuickWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Create RemoteViews for widget with buttons
            val views = RemoteViews(context.packageName, R.layout.widget_quick_translate)
            
            // Setup intents for quick actions
            val translateIntent = Intent(context, MainActivity::class.java).apply {
                action = "ACTION_TRANSLATE"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            val cameraIntent = Intent(context, MainActivity::class.java).apply {
                action = "ACTION_CAMERA"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            val voiceIntent = Intent(context, MainActivity::class.java).apply {
                action = "ACTION_VOICE"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            views.setOnClickPendingIntent(R.id.btnTranslate, 
                PendingIntent.getActivity(context, 0, translateIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            views.setOnClickPendingIntent(R.id.btnCamera, 
                PendingIntent.getActivity(context, 1, cameraIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            views.setOnClickPendingIntent(R.id.btnVoice, 
                PendingIntent.getActivity(context, 2, voiceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}