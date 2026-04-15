package com.translator.universal.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.translator.universal.R

class WidgetRemoteViewsFactory(
    private val context: Context,
    intent: android.content.Intent
) : RemoteViewsService.RemoteViewsFactory {

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    }

    override fun onDataSetChanged() {
        // Reload data
    }

    override fun getCount(): Int = 3 // Quick access buttons

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_item)
        
        when (position) {
            0 -> {
                views.setTextViewText(R.id.widgetItemLabel, "Metin")
                views.setImageViewResource(R.id.widgetItemIcon, R.drawable.ic_translate)
            }
            1 -> {
                views.setTextViewText(R.id.widgetItemLabel, "Kamera")
                views.setImageViewResource(R.id.widgetItemIcon, R.drawable.ic_camera)
            }
            2 -> {
                views.setTextViewText(R.id.widgetItemLabel, "Ses")
                views.setImageViewResource(R.id.widgetItemIcon, R.drawable.ic_mic)
            }
        }
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}

class QuickAccessWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: android.content.Intent): RemoteViewsFactory {
        return WidgetRemoteViewsFactory(this, intent)
    }
}