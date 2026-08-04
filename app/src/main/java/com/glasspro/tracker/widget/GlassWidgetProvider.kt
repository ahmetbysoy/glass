package com.glasspro.tracker.widget

import android.content.Context
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetProvider

class GlassWidgetProvider : HomeWidgetProvider() {
    override fun onUpdate(context: Context) {
        val views = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        views.setTextViewText(android.R.id.text1, "GlassPro Widget")
    }
}
