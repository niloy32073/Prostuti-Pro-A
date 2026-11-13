package com.prostuti.student.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HistoryTimeRangeTest {

    // 2026-11-08 02:00 Dhaka (UTC+6) = 2026-11-07T20:00:00Z — the UTC date is still the 7th.
    private val now = Instant.parse("2026-11-07T20:00:00Z").toEpochMilli()

    @Test
    fun `ALL sends no bounds`() {
        val (from, to) = historyDateBounds(HistoryTimeRange.ALL, now)
        assertNull(from)
        assertNull(to)
    }

    @Test
    fun `TODAY is the whole Dhaka day in UTC`() {
        val (from, to) = historyDateBounds(HistoryTimeRange.TODAY, now)
        assertEquals("2026-11-07T18:00:00", from)
        assertEquals("2026-11-08T17:59:59", to)
    }

    @Test
    fun `LAST_7_DAYS covers today and the six days before`() {
        val (from, to) = historyDateBounds(HistoryTimeRange.LAST_7_DAYS, now)
        assertEquals("2026-11-01T18:00:00", from)
        assertEquals("2026-11-08T17:59:59", to)
    }

    @Test
    fun `LAST_30_DAYS starts 29 days before today`() {
        val (from, _) = historyDateBounds(HistoryTimeRange.LAST_30_DAYS, now)
        assertEquals("2026-10-09T18:00:00", from)
    }

    @Test
    fun `CUSTOM uses the picked dates in either order, and nothing without both`() {
        val (from, to) = historyDateBounds(
            HistoryTimeRange.CUSTOM, now,
            customStart = LocalDate.of(2026, 11, 5), customEnd = LocalDate.of(2026, 10, 20)
        )
        assertEquals("2026-10-19T18:00:00", from)
        assertEquals("2026-11-05T17:59:59", to)

        val (noFrom, noTo) = historyDateBounds(HistoryTimeRange.CUSTOM, now, customStart = LocalDate.of(2026, 11, 5))
        assertNull(noFrom)
        assertNull(noTo)
    }
}
