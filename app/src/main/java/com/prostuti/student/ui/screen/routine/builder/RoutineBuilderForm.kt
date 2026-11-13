package com.prostuti.student.ui.screen.routine.builder

import com.prostuti.student.data.model.enums.Weekday
import com.prostuti.student.util.DhakaTime
import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Step 3's fields (GROUP_ROUTINE_PLAN.md §6.9 / §6.10). Dates are `yyyy-MM-dd` (Asia/Dhaka) so the
 * form can be kept in saved state as JSON. A personal routine keeps the 24-hour window.
 */
@Serializable
data class RoutineBuilderForm(
    val startDate: String,
    val durationDays: Int = DEFAULT_DURATION_DAYS,
    val weekdays: Set<Weekday> = setOf(Weekday.WED, Weekday.SAT),
    val windowOpenMinute: Int = DEFAULT_OPEN,
    val windowCloseMinute: Int = DEFAULT_CLOSE,
    val questionsCount: Int = 25,
    val durationMinutes: Int = 25,
    val markPerQuestion: Double = 1.0,
    val negativeMarking: Boolean = true,
    val negativeMarkValue: Double = 0.25,
    /** ADHOC only. */
    val examDate: String = startDate,
    /** ADHOC only; blank = no title. */
    val title: String = "",
) {
    val totalMarks: Double get() = questionsCount * markPerQuestion
    val windowMinutes: Int get() = windowCloseMinute - windowOpenMinute

    enum class Problem {
        NO_WEEKDAY,
        WINDOW_ORDER,
        DURATION_OVER_WINDOW,
        DATE_IN_PAST,
        WINDOW_ALREADY_OVER,
        TITLE_TOO_LONG,
    }

    /** What stops this form from being sent, checked the same way the server does. */
    fun problems(mode: RoutineBuilderMode, today: LocalDate, nowMinute: Int): Set<Problem> = buildSet {
        if (windowCloseMinute <= windowOpenMinute) add(Problem.WINDOW_ORDER)
        else if (durationMinutes > windowMinutes) add(Problem.DURATION_OVER_WINDOW)
        when (mode) {
            RoutineBuilderMode.ROUTINE -> {
                if (weekdays.isEmpty()) add(Problem.NO_WEEKDAY)
                if ((DhakaTime.parseDate(startDate) ?: today) < today) add(Problem.DATE_IN_PAST)
            }
            RoutineBuilderMode.ADHOC -> {
                val date = DhakaTime.parseDate(examDate) ?: today
                if (date < today) add(Problem.DATE_IN_PAST)
                else if (date == today && windowCloseMinute <= nowMinute) add(Problem.WINDOW_ALREADY_OVER)
                if (title.trim().length > MAX_TITLE) add(Problem.TITLE_TOO_LONG)
            }
        }
    }

    companion object {
        const val DEFAULT_DURATION_DAYS = 30
        const val DEFAULT_OPEN = 19 * 60
        const val DEFAULT_CLOSE = 23 * 60
        const val MAX_TITLE = 255

        const val DAYS_STEP = 7
        const val QUESTIONS_STEP = 5
        const val MINUTES_STEP = 5
        const val WINDOW_STEP = 30
        const val MARK_STEP = 0.25

        val QUESTIONS_RANGE = 5..200
        val DURATION_MINUTES_RANGE = 5..480
        val MARK_RANGE = 0.25..100.0
        val NEGATIVE_RANGE = 0.25..100.0

        /** Design limits (GROUP_ROUTINE_PLAN.md §15.4); the API allows up to 366. */
        fun durationDaysRange(owner: RoutineOwner): IntRange = if (owner is RoutineOwner.Group) 7..90 else 1..90

        fun initial(owner: RoutineOwner, today: LocalDate): RoutineBuilderForm {
            val date = today.toString()
            return if (owner is RoutineOwner.Group) RoutineBuilderForm(startDate = date)
            else RoutineBuilderForm(startDate = date, windowOpenMinute = 0, windowCloseMinute = DhakaTime.MINUTES_PER_DAY)
        }

        /** Rounds to the 0.25 grid so repeated ± steps never drift. */
        fun quarter(value: Double): Double = (value * 4).roundToInt() / 4.0
    }
}
