package com.prostuti.student.domain

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Seconds left on an attempt that started at [startAtIso] and lasts [durationMinutes], clamped
 * to `0..duration`. An unparseable start time falls back to the full duration rather than ending
 * the exam.
 */
fun examRemainingSeconds(startAtIso: String, durationMinutes: Int, nowEpochMillis: Long): Int {
    val totalSeconds = durationMinutes * 60
    val startMillis = parseServerTimeMillis(startAtIso) ?: return totalSeconds
    val elapsedSeconds = ((nowEpochMillis - startMillis) / 1000).coerceAtLeast(0)
    return (totalSeconds - elapsedSeconds).coerceIn(0, totalSeconds.toLong()).toInt()
}

/**
 * Epoch millis of a server timestamp, or null if it can't be read. Accepts ISO-8601 with an
 * offset (`2025-07-01T12:00:00Z`, `…+06:00`) and also without one
 * (`2026-10-29T13:50:22.024000`) — the backend stores times in UTC, so a missing offset means UTC.
 */
fun parseServerTimeMillis(value: String): Long? = try {
    OffsetDateTime.parse(value).toInstant().toEpochMilli()
} catch (e: DateTimeParseException) {
    try {
        LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
    } catch (e: DateTimeParseException) {
        null
    }
}
