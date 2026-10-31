package com.prostuti.student.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ExamRemainingTimeTest {
    private val start = "2025-07-01T12:00:00Z"
    private val startMillis = 1_751_371_200_000L

    @Test fun `full time is left at the start`() =
        assertEquals(3600, examRemainingSeconds(start, 60, startMillis))

    @Test fun `elapsed time is subtracted`() =
        assertEquals(3600 - 45 * 60, examRemainingSeconds(start, 60, startMillis + 45 * 60_000))

    @Test fun `never below zero after the duration`() =
        assertEquals(0, examRemainingSeconds(start, 60, startMillis + 2 * 3_600_000))

    @Test fun `a device clock behind start_at does not add extra time`() =
        assertEquals(3600, examRemainingSeconds(start, 60, startMillis - 60_000))

    @Test fun `offset timestamps are understood`() =
        assertEquals(3600, examRemainingSeconds("2025-07-01T18:00:00+06:00", 60, startMillis))

    @Test fun `an unparseable start time falls back to the full duration`() =
        assertEquals(600, examRemainingSeconds("not a date", 10, startMillis))

    // Regression: the backend sends start_at without an offset; it used to fall back to full time.
    @Test fun `start time without an offset is read as UTC`() =
        assertEquals(3600 - 45 * 60, examRemainingSeconds("2025-07-01T12:00:00", 60, startMillis + 45 * 60_000))

    @Test fun `start time without an offset and with microseconds is read as UTC`() =
        assertEquals(0, examRemainingSeconds("2025-07-01T12:00:00.024000", 25, startMillis + 2 * 86_400_000L))
}
