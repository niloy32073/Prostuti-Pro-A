package com.prostuti.student.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.prostuti.student.data.model.widget.WidgetSnapshot
import com.prostuti.student.data.repository.WidgetSnapshotRepository
import com.prostuti.student.dataStore.PrefKeys
import com.prostuti.student.domain.GetStringFromDataStoreUseCase
import com.prostuti.student.domain.widget.LoadWidgetDataUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Keeps the home screen widget's snapshot fresh. It refreshes when:
 * - the widget draws and the snapshot is older than [STALE_AFTER] (the system redraws it every
 *   30 minutes, see `prostuti_widget_info.xml`);
 * - the user signs in or out ([start]), so another account's data never stays on the home screen;
 * - the user leaves the app ([onAppLeft]), so what they changed inside shows outside;
 * - the user taps the widget's refresh button.
 * Nothing touches the network while no widget is placed.
 */
class WidgetSync(
    private val context: Context,
    private val loadData: LoadWidgetDataUseCase,
    private val snapshots: WidgetSnapshotRepository,
    private val getString: GetStringFromDataStoreUseCase,
    private val appScope: CoroutineScope,
    private val now: () -> Instant = Instant::now,
) {
    private val mutex = Mutex()

    val snapshot: Flow<WidgetSnapshot?> get() = snapshots.observe()

    suspend fun current(): WidgetSnapshot? = snapshots.current()

    /** From [com.prostuti.student.App]: follow sign-in and sign-out (not token rotation). */
    fun start() {
        appScope.launch {
            getString(PrefKeys.JWT_TOKEN)
                .map { !it.isNullOrBlank() }
                .distinctUntilChanged()
                .drop(1)   // the value at app start is no change
                .collect { refreshAndRedraw(maxAge = Duration.ZERO) }
        }
    }

    /** The app went to the background (not a rotation). */
    fun onAppLeft() {
        appScope.launch { refreshAndRedraw(maxAge = LEFT_APP_MIN_AGE) }
    }

    /**
     * Loads and stores a new snapshot unless the last try is younger than [maxAge]. A drawing
     * widget collects [snapshot], so it redraws by itself. True when it loaded.
     */
    suspend fun refresh(maxAge: Duration): Boolean = mutex.withLock {
        val previous = snapshots.current()
        val checkedAgo = now().toEpochMilli() - (previous?.checkedAtMillis ?: 0L)
        if (previous != null && checkedAgo < maxAge.inWholeMilliseconds) return false
        val data = loadData(now())
        snapshots.save(data.toSnapshot(appLanguageContext(context), previous, now()))
        true
    }

    private suspend fun refreshAndRedraw(maxAge: Duration) {
        if (GlanceAppWidgetManager(context).getGlanceIds(ProstutiWidget::class.java).isEmpty()) return
        refresh(maxAge)
        // Widgets without a running session (not drawn lately) draw the new snapshot.
        ProstutiWidget().updateAll(context)
    }

    companion object {
        /** A redraw older than this loads again; a bit under the 30-minute system update. */
        val STALE_AFTER: Duration = 25.minutes

        /** Leaving the app over and over (switching apps) loads at most once a minute. */
        val LEFT_APP_MIN_AGE: Duration = 1.minutes
    }
}
