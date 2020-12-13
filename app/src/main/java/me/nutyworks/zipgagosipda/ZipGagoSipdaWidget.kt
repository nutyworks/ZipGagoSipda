package me.nutyworks.zipgagosipda

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 */
class ZipGagoSipdaWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        context?.let { nnContext ->
            onUpdate(
                nnContext,
                AppWidgetManager.getInstance(nnContext),
                intent?.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: return
            )
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val remaining =
        context.getSharedPreferences(DISPLAY_PREF, MODE_PRIVATE)
            .getLong(TARGET_MILLIS_PREF, System.currentTimeMillis() + 604_800_000).let {
                it - System.currentTimeMillis()
            }
    val widgetText = "집 가기 ${remaining / 60000 / 60}시간 전"

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.zip_gago_sipda_widget)
    views.setTextViewText(R.id.appwidget_text, widgetText)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}