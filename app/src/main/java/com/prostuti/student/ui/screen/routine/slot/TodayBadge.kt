package com.prostuti.student.ui.screen.routine.slot

import com.prostuti.student.data.model.response.RoutineSlot
import java.time.Instant

/** Where a group's exams stand today, for its GroupList card and Home (GROUP_ROUTINE_PLAN.md §6.4, §6.19). */
sealed interface TodayBadge {
    /** A window is open (or I'm mid-exam): the red banner. */
    data class Open(val slot: RoutineSlot) : TodayBadge
    /** Nothing open yet; this one opens next. */
    data class Upcoming(val slot: RoutineSlot) : TodayBadge
    /** Every exam today is over and I took them all. */
    data object Done : TodayBadge
    data object None : TodayBadge

    companion object {
        fun of(todaySlots: List<RoutineSlot>, now: Instant): TodayBadge {
            val byPhase = todaySlots.sortedBy { it.opensAt }.map { it to it.phase(now) }
            byPhase.firstOrNull { (_, p) -> p == SlotPhase.OPEN || p == SlotPhase.IN_PROGRESS }?.let { return Open(it.first) }
            byPhase.firstOrNull { (_, p) -> p == SlotPhase.UPCOMING }?.let { return Upcoming(it.first) }
            if (byPhase.isNotEmpty() && byPhase.all { (_, p) -> p == SlotPhase.ENDED || p == SlotPhase.SUBMITTED }) return Done
            return None
        }
    }
}
