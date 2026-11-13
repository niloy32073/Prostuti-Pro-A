package com.prostuti.student.ui.widget

import android.content.Context
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.widget.WidgetExamItem
import com.prostuti.student.data.model.widget.WidgetExamKind
import com.prostuti.student.data.model.widget.WidgetSnapshot
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.domain.widget.UpcomingExam
import com.prostuti.student.domain.widget.WidgetData
import com.prostuti.student.navigation.AppLink
import com.prostuti.student.util.DhakaTime
import com.prostuti.student.util.LANGUAGE_BANGLA
import java.time.Instant

/**
 * Turns freshly loaded [WidgetData] into the stored snapshot, text in the app language of
 * [context]. A part whose call failed keeps what [previous] had, so a bad connection doesn't
 * blank the widget; signing out drops everything.
 */
internal fun WidgetData.toSnapshot(context: Context, previous: WidgetSnapshot?, now: Instant): WidgetSnapshot = when (this) {
    WidgetData.SignedOut -> WidgetSnapshot(signedIn = false, updatedAtMillis = now.toEpochMilli(), checkedAtMillis = now.toEpochMilli())
    is WidgetData.SignedIn -> {
        val kept = previous?.takeIf { it.signedIn }
        val anyLoaded = upcoming != null || today != null || !access.routine
        WidgetSnapshot(
            signedIn = true,
            updatedAtMillis = if (anyLoaded) now.toEpochMilli() else kept?.updatedAtMillis ?: 0L,
            checkedAtMillis = now.toEpochMilli(),
            exams = upcoming?.map { it.toItem(context, this) } ?: kept?.exams,
            routineOff = !access.routine,
            today = when {
                !access.routine -> emptyList()
                else -> today?.map { it.toItem(context, this) } ?: kept?.today
            },
            hasPersonalRoutine = hasPersonalRoutine ?: kept?.hasPersonalRoutine ?: false,
        )
    }
}

private fun UpcomingExam.toItem(context: Context, data: WidgetData.SignedIn): WidgetExamItem = when (this) {
    is UpcomingExam.Live -> {
        val english = context.resources.configuration.locales[0].language != LANGUAGE_BANGLA
        WidgetExamItem(
            id = exam.examId,
            kind = WidgetExamKind.LIVE,
            title = if (english && exam.titleEn.isNotBlank()) exam.titleEn else exam.titleBn,
            source = context.getString(R.string.widget_source_live),
            startsAtMillis = exam.windowStartEpochMillis,
            endsAtMillis = exam.windowEndEpochMillis,
            link = AppLink.LiveExam(exam.examId).uri,
        )
    }
    is UpcomingExam.Group -> slot.toItem(context, data)
}

private fun RoutineSlot.toItem(context: Context, data: WidgetData.SignedIn): WidgetExamItem {
    val group = groupId
    return WidgetExamItem(
        id = id,
        kind = if (group == null) WidgetExamKind.PERSONAL else WidgetExamKind.GROUP,
        title = title?.takeIf { it.isNotBlank() } ?: context.widgetTopicLabel(data.labels[id] ?: TopicLabel.Count(topicIds.size)),
        source = when (group) {
            null -> context.getString(R.string.widget_source_personal)
            else -> data.groupNames[group] ?: context.getString(R.string.widget_source_group)
        },
        startsAtMillis = DhakaTime.parseInstant(opensAt)?.toEpochMilli() ?: 0L,
        endsAtMillis = DhakaTime.parseInstant(closesAt)?.toEpochMilli() ?: 0L,
        done = myStatus == SlotMyStatus.COMPLETED,
        link = (if (group == null) AppLink.RoutineTab else AppLink.GroupCalendar(group)).uri,
    )
}
