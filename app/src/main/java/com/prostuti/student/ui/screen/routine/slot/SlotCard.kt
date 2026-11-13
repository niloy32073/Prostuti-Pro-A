package com.prostuti.student.ui.screen.routine.slot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.screen.routine.decimalText
import com.prostuti.student.ui.screen.routine.minuteOfDayText
import com.prostuti.student.ui.screen.routine.routineDateText
import com.prostuti.student.ui.screen.routine.topicLabelText
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.DhakaTime
import java.time.Instant

const val TAG_SLOT_CARD_PREFIX = "slot_card_"

/**
 * One scheduled exam: name (or its topics), chips, rules and where its window stands. An open
 * window gets the design's red card. Tapping opens [SlotDetailSheet]. With [onAction] it also gets
 * the exam button for its phase ([SlotExamButtons]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlotCard(
    slot: RoutineSlot,
    label: TopicLabel?,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Shown above the title where slots of several groups mix. */
    groupName: String? = null,
    /** This card's exam is being opened. */
    isBusy: Boolean = false,
    onAction: ((SlotAction) -> Unit)? = null,
) {
    val phase = slot.phase(now)
    val open = phase == SlotPhase.OPEN || phase == SlotPhase.IN_PROGRESS
    val container = if (open) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val onContainer = if (open) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
    val variant = if (open) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val topics = topicLabelText(label ?: TopicLabel.Count(slot.topicIds.size))

    Column(
        modifier = modifier.fillMaxWidth().background(container, RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(14.dp).testTag(TAG_SLOT_CARD_PREFIX + slot.id),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groupName?.let { Text(it, style = ProstutiTypography.bnLabelLarge, color = variant) }
        Text(slot.title ?: topics, style = ProstutiTypography.bnTitleMedium, color = onContainer)
        if (slot.title != null) Text(topics, style = ProstutiTypography.bnBodyMedium, color = variant)
        Text(slotConfigText(slot), style = ProstutiTypography.bnLabelMedium, color = variant)
        val chips = buildList {
            if (slot.kind == SlotKind.ADHOC) add(stringResource(R.string.slot_adhoc_chip))
            if (slot.postponedCount > 0) add(stringResource(R.string.slot_postponed, localizedNumber(slot.postponedCount)))
        }
        if (chips.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { chips.forEach { SlotChip(it, onContainer) } }
        }
        Text(
            slotStatusText(slot, phase, now), style = ProstutiTypography.bnBodyMedium,
            color = if (open) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (onAction != null) SlotExamButtons(phase = phase, isBusy = isBusy, slotId = slot.id, hasExam = slot.examId != null, onAction = onAction)
    }
}

@Composable
private fun SlotChip(text: String, color: Color) {
    Text(
        text, style = ProstutiTypography.bnLabelMedium, color = color,
        modifier = Modifier.background(color.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** "২৫টি প্রশ্ন · ২৫ মিনিট · প্রতি প্রশ্নে ১ মার্ক · নেগেটিভ ০.২৫" (or "নেগেটিভ নেই"). */
@Composable
fun slotConfigText(slot: RoutineSlot): String {
    val q = localizedNumber(slot.questionsCount)
    val d = localizedNumber(slot.durationMinutes)
    val m = decimalText(slot.markPerQuestion)
    return if (slot.negativeMarkValue > 0) stringResource(R.string.slot_config, q, d, m, decimalText(slot.negativeMarkValue))
    else stringResource(R.string.slot_config_no_negative, q, d, m)
}

/** "সন্ধ্যা ৭:০০ – রাত ১১:০০" in Dhaka time. */
@Composable
fun slotWindowText(slot: RoutineSlot): String {
    val opens = DhakaTime.parseInstant(slot.opensAt)?.toEpochMilli()
    val closes = DhakaTime.parseInstant(slot.closesAt)?.toEpochMilli()
    if (opens == null || closes == null) return ""
    // A window closing at midnight reads 24:00, not 00:00 of the next day.
    val closeMinute = DhakaTime.minuteOfDayAt(closes).let { if (it == 0 && DhakaTime.dateAt(closes) != DhakaTime.dateAt(opens)) DhakaTime.MINUTES_PER_DAY else it }
    return stringResource(R.string.slot_window, minuteOfDayText(DhakaTime.minuteOfDayAt(opens)), minuteOfDayText(closeMinute))
}

@Composable
fun slotStatusText(slot: RoutineSlot, phase: SlotPhase, now: Instant): String = when (phase) {
    SlotPhase.UPCOMING -> {
        val opens = DhakaTime.parseInstant(slot.opensAt)
        if (opens == null) stringResource(R.string.slot_status_ended)
        else {
            val time = minuteOfDayText(DhakaTime.minuteOfDayAt(opens.toEpochMilli()))
            val date = DhakaTime.dateAt(opens.toEpochMilli())
            if (date == DhakaTime.dateAt(now.toEpochMilli())) stringResource(R.string.slot_status_upcoming_today, time)
            else stringResource(R.string.slot_status_upcoming_day, routineDateText(date), time)
        }
    }
    SlotPhase.OPEN -> stringResource(R.string.slot_status_open, remainingText(now, DhakaTime.parseInstant(slot.closesAt) ?: now))
    SlotPhase.IN_PROGRESS -> stringResource(R.string.slot_status_in_progress)
    SlotPhase.SUBMITTED -> stringResource(R.string.slot_status_submitted)
    SlotPhase.ENDED -> stringResource(R.string.slot_status_ended)
    SlotPhase.MISSED -> stringResource(R.string.slot_status_missed)
}
