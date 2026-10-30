package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.TimedExamState
import org.junit.Assert.assertEquals
import org.junit.Test

class TimedExamStateDeriverTest {

    private val windowStart = 1_000L
    private val windowEnd = 2_000L

    @Test fun beforeWindowStartIsUpcoming() {
        assertEquals(
            TimedExamState.UPCOMING,
            TimedExamStateDeriver.derive(windowStart, windowEnd, submittedAtEpochMillis = null, nowEpochMillis = 500L)
        )
    }

    @Test fun duringWindowWithNoSubmissionIsOpenNotSubmitted() {
        assertEquals(
            TimedExamState.OPEN_NOT_SUBMITTED,
            TimedExamStateDeriver.derive(windowStart, windowEnd, submittedAtEpochMillis = null, nowEpochMillis = 1_500L)
        )
    }

    @Test fun duringWindowWithSubmissionIsOpenSubmitted() {
        assertEquals(
            TimedExamState.OPEN_SUBMITTED,
            TimedExamStateDeriver.derive(windowStart, windowEnd, submittedAtEpochMillis = 1_200L, nowEpochMillis = 1_500L)
        )
    }

    @Test fun atOrAfterWindowEndIsClosedRegardlessOfSubmission() {
        assertEquals(
            TimedExamState.CLOSED,
            TimedExamStateDeriver.derive(windowStart, windowEnd, submittedAtEpochMillis = null, nowEpochMillis = 2_000L)
        )
        assertEquals(
            TimedExamState.CLOSED,
            TimedExamStateDeriver.derive(windowStart, windowEnd, submittedAtEpochMillis = 1_200L, nowEpochMillis = 5_000L)
        )
    }

    @Test fun submissionTakesPriorityOverUpcomingEdgeCase() {
        // Window start == end instant, submitted exactly at start: closed wins (now >= end).
        assertEquals(
            TimedExamState.CLOSED,
            TimedExamStateDeriver.derive(1_000L, 1_000L, submittedAtEpochMillis = 1_000L, nowEpochMillis = 1_000L)
        )
    }
}
