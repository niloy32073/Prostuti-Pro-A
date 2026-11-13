package com.prostuti.student.ui.screen.routine.slot

import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.util.DhakaTime
import java.time.Instant
import java.time.LocalDate

/**
 * What a slot card shows. `my_status` is the server's truth, but the clock decides the window: a slot
 * whose `closes_at` has passed shows as ended even if the server still says OPEN (the worker that
 * marks MISSED may be late — GROUP_ROUTINE_PLAN.md §7). Nothing is written back.
 */
enum class SlotPhase {
    UPCOMING,
    OPEN,
    /** I started and didn't submit; opening it again submits what I answered (D9). */
    IN_PROGRESS,
    /** Submitted while the window is still open; the result stays locked. */
    SUBMITTED,
    /** The window has closed and I took part. */
    ENDED,
    /** The window has closed and I didn't take part. */
    MISSED,
}

fun RoutineSlot.phase(now: Instant): SlotPhase {
    val opens = DhakaTime.parseInstant(opensAt)
    val closes = DhakaTime.parseInstant(closesAt)
    return when {
        closes != null && !now.isBefore(closes) ->
            if (myStatus == SlotMyStatus.COMPLETED || myStatus == SlotMyStatus.IN_PROGRESS) SlotPhase.ENDED else SlotPhase.MISSED
        myStatus == SlotMyStatus.MISSED -> SlotPhase.MISSED
        opens != null && now.isBefore(opens) -> SlotPhase.UPCOMING
        myStatus == SlotMyStatus.COMPLETED -> SlotPhase.SUBMITTED
        myStatus == SlotMyStatus.IN_PROGRESS -> SlotPhase.IN_PROGRESS
        else -> SlotPhase.OPEN
    }
}

/** Only an exam nobody could have started yet can move (the server answers SLOT_ALREADY_OPEN otherwise). */
fun RoutineSlot.canReschedule(now: Instant): Boolean = phase(now) == SlotPhase.UPCOMING && myStatus == SlotMyStatus.UPCOMING

/** Only one-off exams can be deleted one by one; the server refuses once someone has taken it. */
fun RoutineSlot.canDelete(now: Instant): Boolean = kind == SlotKind.ADHOC && phase(now) == SlotPhase.UPCOMING

val RoutineSlot.date: LocalDate? get() = DhakaTime.parseDate(slotDate)

/** The next moment a window opens or closes after [now], for one automatic refresh then. */
fun List<RoutineSlot>.nextWindowEdge(now: Instant): Instant? =
    flatMap { listOf(it.opensAt, it.closesAt) }.mapNotNull { DhakaTime.parseInstant(it) }.filter { it.isAfter(now) }.minOrNull()
