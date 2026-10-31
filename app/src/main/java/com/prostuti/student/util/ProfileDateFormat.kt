package com.prostuti.student.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Formats an ISO-8601 instant from the API (`2025-01-15T08:00:00Z`) for display, e.g.
 * "15 January 2025" or with [withTime] "20 June 2026, 4:30 PM", in [zone]. Bangla gets Bengali
 * digits. Returns null when [iso] can't be parsed, so the caller can fall back to the raw text.
 */
fun formatApiDateTime(
    iso: String,
    locale: Locale,
    withTime: Boolean = false,
    zone: ZoneId = ZoneId.systemDefault()
): String? {
    val instant = try {
        Instant.parse(iso)
    } catch (_: DateTimeParseException) {
        return null
    }
    val pattern = if (withTime) "d MMMM yyyy, h:mm a" else "d MMMM yyyy"
    val text = DateTimeFormatter.ofPattern(pattern, locale).withZone(zone).format(instant)
    return if (locale.language == LANGUAGE_BANGLA) text.toBengaliDigits() else text
}
