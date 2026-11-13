package com.prostuti.student.ui.screen.routine.slot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.domain.routine.OpenScheduledExamUseCase
import com.prostuti.student.domain.routine.ScheduledExamOutcome
import com.prostuti.student.ui.screen.examengine.ExamAttemptStore
import java.time.Instant

/** One-shot from a screen's ViewModel: open this slot's attempt or result. */
data class ExamLaunch(val slotId: String, val outcome: ScheduledExamOutcome)

/** Runs a slot's exam button; the caller's ViewModel keeps the busy flag and the one-shot. */
suspend fun OpenScheduledExamUseCase.run(slot: RoutineSlot, action: SlotAction, now: Instant): ApiResponse<ScheduledExamOutcome> =
    when (action) {
        SlotAction.TAKE -> take(slot, now)
        SlotAction.VIEW_RESULT -> viewResult(slot, now)
        // The screen opens the ranking itself; nothing to fetch here.
        SlotAction.RANKING -> viewResult(slot, now)
    }

/**
 * Hands a [launch] to the exam engine: a started attempt goes through [ExamAttemptStore] (the engine
 * reads its questions from there) to [onOpenAttempt]; a result to [onOpenResult]. [titleOf] names the
 * exam on those screens.
 */
@Composable
fun HandleExamLaunch(
    launch: ExamLaunch?,
    titleOf: (slotId: String) -> String,
    onConsumed: () -> Unit,
    onOpenAttempt: (attemptId: String, title: String, lockResultUntil: String?) -> Unit,
    onOpenResult: (attemptId: String, title: String, lockResultUntil: String?) -> Unit,
) {
    val title by rememberUpdatedState(titleOf)
    LaunchedEffect(launch) {
        val l = launch ?: return@LaunchedEffect
        onConsumed()
        when (val o = l.outcome) {
            is ScheduledExamOutcome.Attempt -> {
                ExamAttemptStore.put(o.attempt)
                onOpenAttempt(o.attempt.attemptId, title(l.slotId), o.lockResultUntil)
            }
            is ScheduledExamOutcome.Result -> onOpenResult(o.attemptId, title(l.slotId), o.lockResultUntil)
        }
    }
}
