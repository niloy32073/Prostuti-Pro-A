package com.prostuti.student.ui.screen.routine

import androidx.compose.material3.TextButton
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.screen.routine.slot.date
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.screen.routine.slot.SlotPhase
import com.prostuti.student.ui.screen.routine.slot.phase
import com.prostuti.student.ui.theme.ProstutiTypography
import java.time.Instant
import java.time.LocalDate

const val TAG_WEEK_DAY_PREFIX = "week_day_"
const val TAG_WEEK_PREV = "week_prev"
const val TAG_WEEK_NEXT = "week_next"

/** A day's dot in a week strip or day list. */
enum class DayMark { NONE, EXAM, DONE, MISSED }

/** No exam → none; any missed → red; all taken → ✓; otherwise an exam dot. */
fun dayMark(slotsOfDay: List<RoutineSlot>, now: Instant): DayMark {
    if (slotsOfDay.isEmpty()) return DayMark.NONE
    val phases = slotsOfDay.map { it.phase(now) }
    return when {
        SlotPhase.MISSED in phases -> DayMark.MISSED
        phases.all { it == SlotPhase.ENDED || it == SlotPhase.SUBMITTED } -> DayMark.DONE
        else -> DayMark.EXAM
    }
}

/** Shared by the routine tab and the group calendar: which week, which day, and each day's mark. */
data class WeekStripState(
    val weekStart: LocalDate,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val markOf: (LocalDate) -> DayMark,
) {
    val days: List<LocalDate> get() = (0L..6L).map { weekStart.plusDays(it) }
}

/** "← 9 Nov – 15 Nov →" */
@Composable
fun WeekHeader(state: WeekStripState, onShowWeek: (forward: Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShowWeek(false) }, enabled = state.canGoBack, modifier = Modifier.testTag(TAG_WEEK_PREV)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_prev_week))
        }
        Text(
            stringResource(R.string.calendar_week, routineDateText(state.weekStart), routineDateText(state.weekStart.plusDays(6))),
            style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onShowWeek(true) }, enabled = state.canGoForward, modifier = Modifier.testTag(TAG_WEEK_NEXT)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_week))
        }
    }
}

/** The compact strip: the week header over seven day cells (weekday, date, mark). */
@Composable
fun WeekStrip(state: WeekStripState, onShowWeek: (Boolean) -> Unit, onSelectDate: (LocalDate) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WeekHeader(state, onShowWeek)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.days.forEach { day ->
                val selected = day == state.selectedDate
                val (container, content) = dayColors(selected, day == state.today)
                Column(
                    modifier = Modifier.weight(1f).background(container, RoundedCornerShape(12.dp)).clickable { onSelectDate(day) }
                        .padding(vertical = 8.dp).daySemantics(day, selected, state.markOf(day)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(stringResource(day.weekday().labelRes()), style = ProstutiTypography.bnLabelMedium, color = content)
                    Text(localizedNumber(day.dayOfMonth), style = ProstutiTypography.bnTitleMedium, color = content)
                    MarkDot(state.markOf(day), selected, content)
                }
            }
        }
    }
}

/** The wide layouts' day column: the week header over one row per day. */
@Composable
fun WeekDayList(state: WeekStripState, onShowWeek: (Boolean) -> Unit, onSelectDate: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WeekHeader(state, onShowWeek)
        state.days.forEach { day ->
            val selected = day == state.selectedDate
            val (container, content) = dayColors(selected, day == state.today)
            Row(
                modifier = Modifier.fillMaxWidth().background(container, RoundedCornerShape(12.dp)).clickable { onSelectDate(day) }
                    .padding(horizontal = 14.dp, vertical = 12.dp).daySemantics(day, selected, state.markOf(day)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(day.weekday().labelRes()), style = ProstutiTypography.bnBodyMedium, color = content, modifier = Modifier.width(56.dp))
                Text(routineDateText(day), style = ProstutiTypography.bnBodyMedium, color = content, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                MarkDot(state.markOf(day), selected, content)
            }
        }
    }
}

@Composable
private fun dayColors(selected: Boolean, isToday: Boolean): Pair<Color, Color> {
    val container = when {
        selected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    return container to (if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun Modifier.daySemantics(day: LocalDate, selected: Boolean, mark: DayMark): Modifier {
    val examCd = stringResource(R.string.calendar_day_exam_cd)
    return semantics { this.selected = selected; if (mark != DayMark.NONE) contentDescription = examCd }.testTag(TAG_WEEK_DAY_PREFIX + day)
}

@Composable
private fun MarkDot(mark: DayMark, selected: Boolean, content: Color) {
    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
        when (mark) {
            DayMark.NONE -> Unit
            DayMark.EXAM -> Box(Modifier.size(6.dp).background(if (selected) content else MaterialTheme.colorScheme.primary, CircleShape))
            DayMark.MISSED -> Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.error, CircleShape))
            DayMark.DONE -> Icon(Icons.Filled.Check, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        }
    }
}

/** The week's exams in date order, for the third pane on wide screens; a row jumps to its day. */
@Composable
fun WeekExams(slots: List<RoutineSlot>, labels: Map<String, TopicLabel>, onSelectDate: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.routine_tab_week_exams), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp))
        val sorted = slots.sortedBy { it.opensAt }
        if (sorted.isEmpty()) {
            Text(stringResource(R.string.calendar_no_exam), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        sorted.forEach { slot ->
            val d = slot.date
            TextButton(onClick = { d?.let(onSelectDate) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text(d?.let { "${stringResource(it.weekday().labelRes())}, ${routineDateText(it)}" } ?: slot.slotDate,
                        style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(slot.title ?: topicLabelText(labels[slot.id] ?: TopicLabel.Count(slot.topicIds.size)),
                        style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                }
            }
        }
    }
}

