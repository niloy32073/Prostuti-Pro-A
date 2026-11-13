package com.prostuti.student.ui.widget

import android.app.Application
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.material3.ColorProviders
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import com.prostuti.student.data.model.widget.WidgetSnapshot
import com.prostuti.student.ui.theme.DarkColorScheme
import com.prostuti.student.ui.theme.LightColorScheme
import org.koin.android.ext.android.get
import java.time.Instant
import kotlin.time.Duration

/** The widget's three tabs. */
enum class WidgetTab { EXAMS, ROUTINE, PROGRESS }

/** Which tab a placed widget shows; kept per widget in its Glance state. */
internal val WIDGET_TAB_KEY = stringPreferencesKey("widget_tab")
internal val WIDGET_TAB_PARAM = ActionParameters.Key<String>("widget_tab")

/** Brand colors; Glance follows the phone's light/dark mode. */
private val WidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

/** Koin from a widget: Glance creates these classes itself, so there's no constructor injection. */
internal fun Context.widgetSync(): WidgetSync = (applicationContext as Application).get()

/**
 * The Prostuti home screen widget (Glance): upcoming live and group exams, today's routine, and
 * streak / XP / rank (demo values for now). It draws the stored [WidgetSync] snapshot at once
 * and loads a new one when that's stale.
 */
class ProstutiWidget(
    /** Set for a preview (picker, "add to home screen" dialog): draws this, loads nothing. */
    private val preview: WidgetSnapshot? = null,
) : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    /** Android 15+ widget picker: the widget with sample data instead of a blank icon. */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val text = appLanguageContext(context)
        val sample = sampleWidgetSnapshot(text, Instant.now())
        provideContent {
            GlanceTheme(colors = WidgetColors) { WidgetContent(sample, WidgetTab.EXAMS, text, Instant.now()) }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (preview != null) {
            val text = appLanguageContext(context)
            provideContent {
                GlanceTheme(colors = WidgetColors) { WidgetContent(preview, WidgetTab.EXAMS, text, Instant.now()) }
            }
            return
        }
        val sync = context.widgetSync()
        val text = appLanguageContext(context)
        val initial = sync.current()
        provideContent {
            val snapshot by sync.snapshot.collectAsState(initial)
            LaunchedEffect(Unit) { sync.refresh(maxAge = WidgetSync.STALE_AFTER) }
            val tab = currentState<Preferences>()[WIDGET_TAB_KEY]
                ?.let { name -> WidgetTab.entries.firstOrNull { it.name == name } } ?: WidgetTab.EXAMS
            GlanceTheme(colors = WidgetColors) {
                WidgetContent(snapshot = snapshot, tab = tab, text = text, now = Instant.now())
            }
        }
    }
}

class ProstutiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProstutiWidget()
}

/** A tab button: remembers the tab for this widget and redraws it. */
class SelectWidgetTabAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val tab = parameters[WIDGET_TAB_PARAM] ?: return
        updateAppWidgetState(context, glanceId) { it[WIDGET_TAB_KEY] = tab }
        ProstutiWidget().update(context, glanceId)
    }
}

/** The refresh button: loads now, whatever the snapshot's age. */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.widgetSync().refresh(maxAge = Duration.ZERO)
        ProstutiWidget().update(context, glanceId)
    }
}
