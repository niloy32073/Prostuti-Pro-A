package com.prostuti.student.ui.widget

import android.content.Context
import com.prostuti.student.R
import com.prostuti.student.data.model.widget.WidgetExamItem
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.util.DhakaTime
import com.prostuti.student.util.LANGUAGE_BANGLA
import com.prostuti.student.util.LocaleManager
import com.prostuti.student.util.formatMinuteOfDayBn
import com.prostuti.student.util.toBengaliDigits
import java.time.Instant
import java.time.LocalTime
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

// Widget text in the app's language. A widget draws outside the app's activity, so it can't use
// the Compose helpers in RoutineText.kt (they read LocalConfiguration); these take a Context that
// carries the app language instead (see [appLanguageContext]).

/** [context] with the app's own language (Bangla by default), not the phone's. */
internal fun appLanguageContext(context: Context): Context = LocaleManager.wrapContext(context)

private fun Context.isBangla(): Boolean = resources.configuration.locales[0].language == LANGUAGE_BANGLA

/** 1240 → "১,২৪০" / "1,240". */
internal fun Context.widgetNumber(value: Int): String {
    val text = NumberFormat.getIntegerInstance(Locale.US).format(value)
    return if (isBangla()) text.toBengaliDigits() else text
}

/** "সন্ধ্যা ৭:০০" / "7:00 PM", Dhaka time. */
internal fun Context.widgetClock(epochMillis: Long): String {
    val minute = DhakaTime.minuteOfDayAt(epochMillis)
    if (isBangla()) return formatMinuteOfDayBn(minute)
    return DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH).format(LocalTime.of(minute / 60, minute % 60))
}

/** "চলছে · শেষ রাত ৯:০০", "আজ সন্ধ্যা ৭:০০", "আগামীকাল …", "১৫ নভেম্বর, …". */
internal fun Context.widgetExamTime(item: WidgetExamItem, now: Instant): String {
    val nowMillis = now.toEpochMilli()
    if (item.startsAtMillis <= nowMillis && nowMillis < item.endsAtMillis) {
        return getString(R.string.widget_live_now, widgetClock(item.endsAtMillis))
    }
    val today = DhakaTime.dateAt(nowMillis)
    val day = DhakaTime.dateAt(item.startsAtMillis)
    val clock = widgetClock(item.startsAtMillis)
    return when (day) {
        today -> getString(R.string.widget_today_at, clock)
        today.plusDays(1) -> getString(R.string.widget_tomorrow_at, clock)
        else -> {
            val date = DateTimeFormatter.ofPattern("d MMMM", resources.configuration.locales[0]).format(day)
            getString(R.string.widget_date_at, if (isBangla()) date.toBengaliDigits() else date, clock)
        }
    }
}

/** Same wording as the routine screens' topic label (RoutineText.kt `topicLabelText`). */
internal fun Context.widgetTopicLabel(label: TopicLabel): String {
    val language = resources.configuration.locales[0].language
    return when (label) {
        is TopicLabel.Chapter -> {
            val chapter = label.chapter.inLanguage(language)
            val base = label.subject?.let { getString(R.string.routine_topic_chapter, it.inLanguage(language), chapter) } ?: chapter
            if (label.partialTopics.isEmpty()) base
            else {
                val topics = label.partialTopics.joinToString(", ") { it.inLanguage(language) }
                val list = if (label.moreTopics > 0) getString(R.string.routine_topic_more, topics, widgetNumber(label.moreTopics)) else topics
                getString(R.string.routine_topic_partial, base, list)
            }
        }
        is TopicLabel.Chapters -> {
            val chapters = label.chapters.joinToString(", ") { it.inLanguage(language) }
            val list = if (label.more > 0) getString(R.string.routine_topic_more, chapters, widgetNumber(label.more)) else chapters
            label.subject?.let { getString(R.string.routine_topic_chapter, it.inLanguage(language), list) } ?: list
        }
        is TopicLabel.Topics -> {
            val topics = label.topics.joinToString(", ") { it.inLanguage(language) }
            if (label.more > 0) getString(R.string.routine_topic_more, topics, widgetNumber(label.more)) else topics
        }
        is TopicLabel.Count -> getString(R.string.routine_topic_count, widgetNumber(label.topicCount))
    }
}
