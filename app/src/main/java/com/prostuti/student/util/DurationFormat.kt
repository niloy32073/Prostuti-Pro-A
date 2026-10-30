package com.prostuti.student.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** "৩ ঘণ্টা", "৪৫ মিনিট" — used for every exam-window countdown in the app (GroupDetail,
 * LiveExamDetail, LockedResult). */
fun formatRemainingBn(millis: Long): String {
    if (millis <= 0) return "কিছুক্ষণের মধ্যে"
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours.toBengaliDigits()} ঘণ্টা ${minutes.toBengaliDigits()} মিনিট"
        hours > 0 -> "${hours.toBengaliDigits()} ঘণ্টা"
        else -> "${minutes.coerceAtLeast(1).toBengaliDigits()} মিনিট"
    }
}

private fun Long.toBengaliDigits(): String = toString().toBengaliDigits()

/** "সন্ধ্যা ৭:০০", "রাত ১১:০০", "সকাল ৮:৩০" — minuteOfDay is 0..1439. */
fun formatMinuteOfDayBn(minuteOfDay: Int): String {
    val h24 = (minuteOfDay / 60) % 24
    val m = minuteOfDay % 60
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    val period = when (h24) {
        in 4..11 -> "সকাল"
        in 12..15 -> "দুপুর"
        in 16..18 -> "বিকাল"
        in 19..20 -> "সন্ধ্যা"
        else -> "রাত"
    }
    return "$period ${h12.toBengaliDigits()}:${"%02d".format(m).toBengaliDigits()}"
}

/** "আজ, সন্ধ্যা ৭:০০" / "আগামীকাল, রাত ৮:০০" — an absolute window boundary, for LiveExamDetail's
 * info table (unlike GroupDetail's countdown, which is always relative — "আরও X বাকি"). */
fun formatAbsoluteDateTimeBn(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val today = LocalDate.now(zone)
    val datePart = when (dateTime.toLocalDate()) {
        today -> "আজ"
        today.plusDays(1) -> "আগামীকাল"
        else -> "${dateTime.dayOfMonth.toBengaliDigits()}/${dateTime.monthValue.toBengaliDigits()}"
    }
    val minuteOfDay = dateTime.hour * 60 + dateTime.minute
    return "$datePart, ${formatMinuteOfDayBn(minuteOfDay)}"
}
