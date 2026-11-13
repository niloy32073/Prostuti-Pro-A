package com.prostuti.student.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.prostuti.student.domain.widget.HomeWidgetHost
import java.time.Instant

/**
 * [HomeWidgetHost] through Glance. The "add to home screen" dialog shows the widget filled with
 * sample data ([sampleWidgetSnapshot]), so people see what they get before they add it.
 */
class GlanceHomeWidgetHost(private val context: Context) : HomeWidgetHost {

    override fun canPin(): Boolean = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    override suspend fun isPlaced(): Boolean =
        GlanceAppWidgetManager(context).getGlanceIds(ProstutiWidget::class.java).isNotEmpty()

    override suspend fun requestPin(): Boolean {
        if (!canPin()) return false
        val preview = ProstutiWidget(preview = sampleWidgetSnapshot(appLanguageContext(context), Instant.now()))
        return GlanceAppWidgetManager(context).requestPinGlanceAppWidget(ProstutiWidgetReceiver::class.java, preview = preview)
    }
}
