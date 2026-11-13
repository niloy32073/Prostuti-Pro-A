package com.prostuti.student.ui.screen.routine.slot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.prostuti.student.R
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.ui.component.localizedNumber
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private const val TICK_MS = 30_000L
/** A little past the edge, so the server has already moved the slot on. */
private const val EDGE_GRACE_MS = 2_000L

/** The current time, ticking every 30 s while the screen is shown — enough for "X minutes left". */
@Composable
fun rememberNow(): State<Instant> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(Instant.now(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = Instant.now()
                delay(TICK_MS)
            }
        }
    }
}

/**
 * Reloads once when the next window among [slots] opens or closes (GROUP_ROUTINE_PLAN.md §7), so the
 * card never keeps showing a closed window as open. Paused while the screen isn't resumed; on
 * coming back the wait starts again from the current time.
 */
@Composable
fun RefreshAtWindowEdges(slots: List<RoutineSlot>, onRefresh: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val refresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(slots, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val edge = slots.nextWindowEdge(Instant.now()) ?: return@repeatOnLifecycle
            delay(Duration.between(Instant.now(), edge).toMillis() + EDGE_GRACE_MS)
            refresh()
        }
    }
}

/** "২ ঘণ্টা ১০ মিনিট" / "2 h 10 min"; at least one minute. */
@Composable
fun remainingText(from: Instant, until: Instant): String {
    val minutes = Duration.between(from, until).toMinutes().coerceAtLeast(1)
    val hours = (minutes / 60).toInt()
    val rest = (minutes % 60).toInt()
    return when {
        hours > 0 && rest > 0 -> stringResource(R.string.slot_remaining_hm, localizedNumber(hours), localizedNumber(rest))
        hours > 0 -> stringResource(R.string.slot_remaining_h, localizedNumber(hours))
        else -> stringResource(R.string.slot_remaining_m, localizedNumber(rest))
    }
}
