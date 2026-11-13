package com.prostuti.student.ui.widget

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.prostuti.student.BuildConfig
import com.prostuti.student.R
import com.prostuti.student.data.model.widget.WidgetExamItem
import com.prostuti.student.data.model.widget.WidgetExamKind
import com.prostuti.student.data.model.widget.WidgetSnapshot
import com.prostuti.student.data.repository.StorageRepository
import com.prostuti.student.dataStore.PrefKeys
import com.prostuti.student.navigation.AppLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * What the widget looks like with data, for the widget picker and the "add to home screen"
 * dialog: one running live exam, a group exam later today, a personal one tomorrow.
 */
internal fun sampleWidgetSnapshot(text: Context, now: Instant): WidgetSnapshot {
    fun item(id: String, kind: WidgetExamKind, title: Int, source: String, startsIn: Long, link: AppLink) = WidgetExamItem(
        id = id, kind = kind, title = text.getString(title), source = source,
        startsAtMillis = now.plus(startsIn, ChronoUnit.MINUTES).toEpochMilli(),
        endsAtMillis = now.plus(startsIn + 60, ChronoUnit.MINUTES).toEpochMilli(),
        link = link.uri,
    )
    val live = item("sample-live", WidgetExamKind.LIVE, R.string.widget_sample_live_title, text.getString(R.string.widget_source_live), -20, AppLink.LiveExams)
    val group = item("sample-group", WidgetExamKind.GROUP, R.string.widget_sample_group_title, text.getString(R.string.widget_sample_group_name), 180, AppLink.RoutineTab)
    val personal = item("sample-personal", WidgetExamKind.PERSONAL, R.string.widget_sample_personal_title, text.getString(R.string.widget_source_personal), 24 * 60, AppLink.RoutineTab)
    return WidgetSnapshot(
        signedIn = true,
        updatedAtMillis = now.toEpochMilli(),
        checkedAtMillis = now.toEpochMilli(),
        exams = listOf(live, group, personal),
        today = listOf(live.copy(kind = WidgetExamKind.PERSONAL), group),
        hasPersonalRoutine = true,
    )
}

/**
 * Android 15+ widget pickers show a live preview the app publishes ([ProstutiWidget.providePreview]).
 * The system rate-limits publishing, so it's done once per app version.
 */
class WidgetPreviewPublisher(
    private val context: Context,
    private val storage: StorageRepository,
    private val appScope: CoroutineScope,
) {
    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        appScope.launch {
            if (storage.getLong(PrefKeys.WIDGET_PREVIEW_VERSION).first() == BuildConfig.VERSION_CODE.toLong()) return@launch
            val result = GlanceAppWidgetManager(context).setWidgetPreviews(ProstutiWidgetReceiver::class)
            if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
                storage.putLong(PrefKeys.WIDGET_PREVIEW_VERSION, BuildConfig.VERSION_CODE.toLong())
            } else {
                Timber.w("Widget preview not published (rate limited); next app start tries again")
            }
        }
    }
}
