package com.prostuti.student.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Time rules of the groups/routines API: every timestamp (`opens_at`, `closes_at`, `created_at`)
 * is UTC, usually sent with no offset (`2026-11-10T18:00:00`); every calendar day (`slot_date`,
 * `start_date`, `exam_date`) and every "minute of day" is Asia/Dhaka local time.
 */
object DhakaTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Dhaka")

    /** Minutes in a day; `window_close_minute` may be exactly this (midnight at the day's end). */
    const val MINUTES_PER_DAY = 1440

    /** Parses an API timestamp as UTC. Accepts the offset-less form and, to be safe, `Z`/offset forms. */
    fun parseInstant(value: String): Instant? = try {
        LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(value).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** `yyyy-MM-dd` from the API, or null when malformed. */
    fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }

    /** The Dhaka calendar day at [epochMillis] — "today" for the API, whatever the phone's time zone. */
    fun dateAt(epochMillis: Long): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDate()

    /** Minutes after Dhaka midnight at [epochMillis] (0..1439). */
    fun minuteOfDayAt(epochMillis: Long): Int {
        val time = Instant.ofEpochMilli(epochMillis).atZone(ZONE)
        return time.hour * 60 + time.minute
    }

    /** The instant [minuteOfDay] minutes after Dhaka midnight of [date]; 1440 gives the next midnight. */
    fun instantOf(date: LocalDate, minuteOfDay: Int): Instant =
        date.atStartOfDay(ZONE).plusMinutes(minuteOfDay.toLong()).toInstant()
}
