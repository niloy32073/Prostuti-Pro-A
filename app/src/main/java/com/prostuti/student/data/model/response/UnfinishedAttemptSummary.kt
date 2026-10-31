package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.AttemptMode

/**
 * What Home's "continue" card needs about the newest unfinished attempt. Built from
 * `GET /exams/attempts/manage?unfinished=true` plus `GET /exams/{exam_id}` for the title and
 * duration; [titleBn]/[durationMinutes] are null when the exam lookup failed.
 */
data class UnfinishedAttemptSummary(
    val attemptId: String,
    val examId: String,
    val mode: AttemptMode,
    val startAt: String,
    val titleBn: String?,
    val durationMinutes: Int?,
    /** Exam time already used, in ms (practice time pauses while the exam is closed). */
    val usedMs: Long,
    /** How many other unfinished attempts exist besides this one. */
    val otherUnfinishedCount: Int
)
