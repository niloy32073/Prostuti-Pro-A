package com.prostuti.student.domain.widget

import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.domain.FeatureAccess
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.util.DhakaTime
import java.time.Instant
import java.time.LocalDate

/** What the home screen widget shows, before it turns into text. */
sealed interface WidgetData {
    data object SignedOut : WidgetData

    data class SignedIn(
        val access: FeatureAccess,
        /** Running exams first, then the next ones; null = the calls failed. */
        val upcoming: List<UpcomingExam>?,
        /** Today's exams, personal and group; null = the call failed or routines are off. */
        val today: List<RoutineSlot>?,
        /** Null = unknown (the call failed or routines are off). */
        val hasPersonalRoutine: Boolean?,
        val groupNames: Map<String, String>,
        val labels: Map<String, TopicLabel>,
    ) : WidgetData
}

/** One exam on the widget's "upcoming" tab: the whole stream's live exam or one of my groups'. */
sealed interface UpcomingExam {
    val startsAt: Instant
    val endsAt: Instant

    data class Live(val exam: TimedExam) : UpcomingExam {
        override val startsAt: Instant get() = Instant.ofEpochMilli(exam.windowStartEpochMillis)
        override val endsAt: Instant get() = Instant.ofEpochMilli(exam.windowEndEpochMillis)
    }

    data class Group(val slot: RoutineSlot, override val startsAt: Instant, override val endsAt: Instant) : UpcomingExam

    fun isRunning(now: Instant): Boolean = startsAt <= now && now < endsAt
}

/** How many exams the "upcoming" tab keeps. */
const val WIDGET_UPCOMING_LIMIT = 3

/**
 * Live exams and my groups' exams that haven't ended, running ones first, then by start time.
 * A group exam I already finished or missed is left out; personal exams aren't "live" exams.
 */
fun pickUpcoming(live: List<TimedExam>, slots: List<RoutineSlot>, now: Instant, limit: Int = WIDGET_UPCOMING_LIMIT): List<UpcomingExam> {
    val group = slots
        .filter { it.groupId != null && it.myStatus != SlotMyStatus.COMPLETED && it.myStatus != SlotMyStatus.MISSED }
        .mapNotNull { slot ->
            val start = DhakaTime.parseInstant(slot.opensAt) ?: return@mapNotNull null
            val end = DhakaTime.parseInstant(slot.closesAt) ?: return@mapNotNull null
            UpcomingExam.Group(slot, start, end)
        }
    return (live.map { UpcomingExam.Live(it) } + group)
        .filter { it.endsAt > now }
        .sortedWith(compareBy<UpcomingExam>({ !it.isRunning(now) }, { it.startsAt }))
        .take(limit)
}

/** Today's exams (personal and group), earliest first. */
fun pickToday(slots: List<RoutineSlot>, today: LocalDate): List<RoutineSlot> =
    slots.filter { it.slotDate == today.toString() }.sortedBy { it.opensAt }
