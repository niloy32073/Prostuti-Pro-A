package com.prostuti.student.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DhakaTimeTest {
    @Test fun `offset-less timestamps are read as UTC`() {
        assertEquals(Instant.parse("2026-11-10T18:00:00Z"), DhakaTime.parseInstant("2026-11-10T18:00:00"))
    }

    @Test fun `timestamps with Z or an offset also parse`() {
        assertEquals(Instant.parse("2026-11-10T18:00:00Z"), DhakaTime.parseInstant("2026-11-10T18:00:00Z"))
        assertEquals(Instant.parse("2026-11-10T18:00:00Z"), DhakaTime.parseInstant("2026-11-11T00:00:00+06:00"))
    }

    @Test fun `fractional seconds parse`() {
        assertEquals(Instant.parse("2026-11-10T18:00:00.500Z"), DhakaTime.parseInstant("2026-11-10T18:00:00.500"))
    }

    @Test fun `malformed values give null`() {
        assertNull(DhakaTime.parseInstant("not a time"))
        assertNull(DhakaTime.parseDate("2026-13-40"))
    }

    @Test fun `dates parse`() {
        assertEquals(LocalDate.of(2026, 11, 15), DhakaTime.parseDate("2026-11-15"))
    }

    @Test fun `the Dhaka day changes at 18_00 UTC`() {
        val beforeMidnight = Instant.parse("2026-11-10T17:59:00Z").toEpochMilli()
        val afterMidnight = Instant.parse("2026-11-10T18:00:00Z").toEpochMilli()
        assertEquals(LocalDate.of(2026, 11, 10), DhakaTime.dateAt(beforeMidnight))
        assertEquals(LocalDate.of(2026, 11, 11), DhakaTime.dateAt(afterMidnight))
    }

    @Test fun `minute of day is Dhaka local`() {
        // 13:00 UTC = 19:00 in Dhaka.
        assertEquals(19 * 60, DhakaTime.minuteOfDayAt(Instant.parse("2026-11-10T13:00:00Z").toEpochMilli()))
    }

    @Test fun `instantOf maps a Dhaka minute of day back to UTC`() {
        val day = LocalDate.of(2026, 11, 10)
        assertEquals(Instant.parse("2026-11-10T00:00:00Z"), DhakaTime.instantOf(day, 360)) // 06:00 Dhaka
        assertEquals(Instant.parse("2026-11-10T18:00:00Z"), DhakaTime.instantOf(day, DhakaTime.MINUTES_PER_DAY))
    }
}
