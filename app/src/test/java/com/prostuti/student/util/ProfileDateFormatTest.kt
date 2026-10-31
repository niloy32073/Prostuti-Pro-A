package com.prostuti.student.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class ProfileDateFormatTest {
    private val dhaka = ZoneId.of("Asia/Dhaka")

    @Test fun `english date`() {
        assertEquals("15 January 2025", formatApiDateTime("2025-01-15T08:00:00Z", Locale.ENGLISH, zone = dhaka))
    }

    @Test fun `english date with time in the local zone`() {
        // 10:30 UTC = 16:30 in Dhaka (UTC+6).
        assertEquals(
            "20 June 2026, 4:30 PM",
            formatApiDateTime("2026-06-20T10:30:00Z", Locale.ENGLISH, withTime = true, zone = dhaka)
        )
    }

    @Test fun `bangla uses bengali digits`() {
        val text = formatApiDateTime("2025-01-15T08:00:00Z", Locale.Builder().setLanguage("bn").build(), zone = dhaka)!!
        assertEquals("১৫", text.substringBefore(' '))
        assertEquals("২০২৫", text.substringAfterLast(' '))
    }

    @Test fun `unparseable value returns null`() {
        assertNull(formatApiDateTime("not a date", Locale.ENGLISH, zone = dhaka))
    }
}
