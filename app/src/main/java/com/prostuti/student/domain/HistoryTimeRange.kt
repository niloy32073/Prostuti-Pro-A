package com.prostuti.student.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** The "time" filter of the exam history. Days are Asia/Dhaka calendar days. */
enum class HistoryTimeRange { ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS, CUSTOM }

private val DHAKA: ZoneId = ZoneId.of("Asia/Dhaka")
// UTC without an offset, the same shape the server sends (`2026-11-08T05:18:05`). The test server
// answers 500 INTERNAL_ERROR when the value carries a `Z` offset.
private val UTC_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

/**
 * `date_from` / `date_to` (UTC ISO-8601, no offset) for [range]. "Last 7 days" = today and the 6 days
 * before it. [customStart]/[customEnd] are the picked Dhaka dates (inclusive), used for CUSTOM.
 * Returns nulls for ALL, or for CUSTOM without both dates.
 */
fun historyDateBounds(
    range: HistoryTimeRange,
    nowMillis: Long,
    customStart: LocalDate? = null,
    customEnd: LocalDate? = null
): Pair<String?, String?> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(DHAKA).toLocalDate()
    val (from, to) = when (range) {
        HistoryTimeRange.ALL -> return null to null
        HistoryTimeRange.TODAY -> today to today
        HistoryTimeRange.LAST_7_DAYS -> today.minusDays(6) to today
        HistoryTimeRange.LAST_30_DAYS -> today.minusDays(29) to today
        HistoryTimeRange.CUSTOM -> {
            if (customStart == null || customEnd == null) return null to null
            minOf(customStart, customEnd) to maxOf(customStart, customEnd)
        }
    }
    val fromInstant = from.atStartOfDay(DHAKA).toInstant()
    val toInstant = to.plusDays(1).atStartOfDay(DHAKA).toInstant().minusSeconds(1)
    return UTC_FORMAT.format(fromInstant.atOffset(ZoneOffset.UTC)) to UTC_FORMAT.format(toInstant.atOffset(ZoneOffset.UTC))
}
