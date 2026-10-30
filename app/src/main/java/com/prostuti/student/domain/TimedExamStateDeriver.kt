package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.TimedExamState

/**
 * The single rule every live/group exam state in the app comes from — never stored directly.
 * Kept as a pure function (no DI, no suspend) so it's trivial to unit test against the four
 * window/submission combinations.
 */
object TimedExamStateDeriver {
    fun derive(
        windowStartEpochMillis: Long,
        windowEndEpochMillis: Long,
        submittedAtEpochMillis: Long?,
        nowEpochMillis: Long
    ): TimedExamState = when {
        nowEpochMillis < windowStartEpochMillis -> TimedExamState.UPCOMING
        nowEpochMillis >= windowEndEpochMillis -> TimedExamState.CLOSED
        submittedAtEpochMillis != null -> TimedExamState.OPEN_SUBMITTED
        else -> TimedExamState.OPEN_NOT_SUBMITTED
    }
}
