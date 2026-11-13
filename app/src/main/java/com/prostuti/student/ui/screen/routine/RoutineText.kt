package com.prostuti.student.ui.screen.routine

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.Weekday
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.util.LANGUAGE_BANGLA
import com.prostuti.student.util.formatMinuteOfDayBn
import com.prostuti.student.util.toBengaliDigits
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.prostuti.student.ui.component.appLanguage

// Routine and slot text in the app language: Bangla words and digits, or English.

@Composable
@ReadOnlyComposable
private fun isBangla(): Boolean = LocalConfiguration.current.locales[0].language == LANGUAGE_BANGLA

/** "সন্ধ্যা ৭:০০" / "7:00 PM"; 1440 is midnight at the end of the day. */
@Composable
@ReadOnlyComposable
internal fun minuteOfDayText(minuteOfDay: Int): String {
    if (isBangla()) return formatMinuteOfDayBn(minuteOfDay)
    val time = LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60)
    return DateTimeFormatter.ofPattern("h:mm a", LocalConfiguration.current.locales[0]).format(time)
}

/** "১৫ নভেম্বর" / "15 November". */
@Composable
@ReadOnlyComposable
internal fun routineDateText(date: LocalDate): String {
    val text = DateTimeFormatter.ofPattern("d MMMM", LocalConfiguration.current.locales[0]).format(date)
    return if (isBangla()) text.toBengaliDigits() else text
}

/** 1.0 → "1", 0.25 → "0.25", in the app's digits. */
@Composable
@ReadOnlyComposable
internal fun decimalText(value: Double): String {
    val plain = if (value == Math.floor(value)) value.toLong().toString() else "%.2f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')
    return if (isBangla()) plain.toBengaliDigits() else plain
}

@StringRes
internal fun Weekday.labelRes(): Int = when (this) {
    Weekday.MON -> R.string.routine_weekday_mon
    Weekday.TUE -> R.string.routine_weekday_tue
    Weekday.WED -> R.string.routine_weekday_wed
    Weekday.THU -> R.string.routine_weekday_thu
    Weekday.FRI -> R.string.routine_weekday_fri
    Weekday.SAT -> R.string.routine_weekday_sat
    Weekday.SUN -> R.string.routine_weekday_sun
}

internal fun LocalDate.weekday(): Weekday = Weekday.entries[dayOfWeek.value - 1]

@Composable
internal fun topicLabelText(label: TopicLabel): String {
    val language = appLanguage()
    return when (label) {
        is TopicLabel.Chapter -> {
            val chapter = label.chapter.inLanguage(language)
            val base = label.subject?.let { stringResource(R.string.routine_topic_chapter, it.inLanguage(language), chapter) } ?: chapter
            if (label.partialTopics.isEmpty()) base
            else {
                val topics = label.partialTopics.joinToString(", ") { it.inLanguage(language) }
                val list = if (label.moreTopics > 0) stringResource(R.string.routine_topic_more, topics, localizedNumber(label.moreTopics)) else topics
                stringResource(R.string.routine_topic_partial, base, list)
            }
        }
        is TopicLabel.Chapters -> {
            val chapters = label.chapters.joinToString(", ") { it.inLanguage(language) }
            val list = if (label.more > 0) stringResource(R.string.routine_topic_more, chapters, localizedNumber(label.more)) else chapters
            label.subject?.let { stringResource(R.string.routine_topic_chapter, it.inLanguage(language), list) } ?: list
        }
        is TopicLabel.Topics -> {
            val topics = label.topics.joinToString(", ") { it.inLanguage(language) }
            if (label.more > 0) stringResource(R.string.routine_topic_more, topics, localizedNumber(label.more)) else topics
        }
        is TopicLabel.Count -> stringResource(R.string.routine_topic_count, localizedNumber(label.topicCount))
    }
}
