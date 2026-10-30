package com.prostuti.student.data.model.response

import kotlinx.serialization.Serializable

/** The read-only summary shown on GroupDetail/GroupRoutineCalendar once an admin has created and
 * locked the group's routine — never editable after creation (same rule the window-config step
 * enforces). */
@Serializable
data class GroupRoutineSummary(
    val subjectCount: Int,
    val chapterCount: Int,
    val topicCount: Int,
    val routineDurationDays: Int,
    /** Bit i set (i = 0..6, সোম..রবি) when the group has an exam that weekday. */
    val examWeekDaysBitmask: Int,
    val windowStartMinuteOfDay: Int,
    val windowEndMinuteOfDay: Int,
    val questionCount: Int,
    val markPerQuestion: Float,
    val negativeMarkingEnabled: Boolean,
    val negativeMark: Float,
    val examDurationMinutes: Int
) {
    val totalMarks: Float get() = questionCount * markPerQuestion
}

/** What an admin submits from the 3-step routine builder. */
@Serializable
data class CreateGroupRoutineRequest(
    val subjectIds: List<String>,
    val chapterIds: List<String>,
    val topicIds: List<String>,
    val routineDurationDays: Int,
    val examWeekDaysBitmask: Int,
    val questionCount: Int,
    val markPerQuestion: Float,
    val negativeMarkingEnabled: Boolean,
    val negativeMark: Float,
    val examDurationMinutes: Int,
    val windowStartMinuteOfDay: Int,
    val windowEndMinuteOfDay: Int
)
