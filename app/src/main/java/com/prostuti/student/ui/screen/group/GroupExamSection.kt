package com.prostuti.student.ui.screen.group

import com.prostuti.student.ui.screen.routine.slot.PrepareCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.screen.routine.routineDateText
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import com.prostuti.student.ui.screen.routine.slot.SlotCard
import com.prostuti.student.ui.screen.routine.slot.date
import com.prostuti.student.ui.theme.ProstutiTypography
import java.time.Instant

const val TAG_EXAM_SECTION = "group_exam_section"
const val TAG_NO_EXAM_TODAY = "group_no_exam_today"
const val TAG_OPEN_CALENDAR = "group_open_calendar"

/**
 * GroupDetail's "গ্রুপ এক্সাম" block (GROUP_ROUTINE_PLAN.md §6.5): a card per exam today; otherwise
 * "no exam today — next …" (or that nothing is set yet). Admins also get "create routine" while none
 * is active and "schedule a one-off exam". A calendar link once there is anything to show.
 */
@Composable
internal fun GroupExamSection(
    state: GroupDetailUiState,
    now: Instant,
    onOpenSlot: (String) -> Unit,
    onSlotAction: (String, SlotAction) -> Unit,
    onOpenCalendar: () -> Unit,
    onCreateRoutine: () -> Unit,
    onScheduleAdhoc: () -> Unit,
    onConcepts: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag(TAG_EXAM_SECTION)) {
        Text(stringResource(R.string.group_exam_section), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface)

        if (state.todaySlots.isNotEmpty()) {
            state.todaySlots.forEach { slot ->
                SlotCard(
                    slot = slot, label = state.labels[slot.id], now = now, onClick = { onOpenSlot(slot.id) },
                    isBusy = state.busyExamSlotId == slot.id, onAction = { onSlotAction(slot.id, it) },
                )
            }
        } else {
            val next = state.nextSlot?.date
            val text = when {
                next != null -> stringResource(R.string.group_no_exam_today_next, routineDateText(next))
                state.routine != null -> stringResource(R.string.group_no_exam_today)
                state.isAdmin -> stringResource(R.string.group_no_routine_admin)
                else -> stringResource(R.string.group_member_no_routine)
            }
            Text(
                text, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(14.dp))
                    .padding(16.dp).testTag(TAG_NO_EXAM_TODAY)
            )
            // Every member, not only the admin, needs to know what the next exam covers.
            state.nextSlot?.let { PrepareCard(it, state.labels[it.id], onConcepts) }
        }

        if (state.isAdmin) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag(TAG_ADMIN_SCHEDULE)) {
                if (state.hasActiveRoutine == false || (state.hasActiveRoutine == null && !state.hasSchedule)) {
                    PrimaryButton(text = stringResource(R.string.group_create_routine), onClick = onCreateRoutine, modifier = Modifier.testTag(TAG_CREATE_ROUTINE))
                }
                OutlinedButton(onClick = onScheduleAdhoc, modifier = Modifier.fillMaxWidth().testTag(TAG_SCHEDULE_ADHOC)) {
                    Text(stringResource(R.string.group_schedule_adhoc), style = ProstutiTypography.bnLabelLarge)
                }
            }
        }

        if (state.hasSchedule) {
            TextButton(onClick = onOpenCalendar, modifier = Modifier.testTag(TAG_OPEN_CALENDAR)) {
                Text(stringResource(R.string.group_open_calendar), style = ProstutiTypography.bnLabelLarge)
            }
        }
    }
}
