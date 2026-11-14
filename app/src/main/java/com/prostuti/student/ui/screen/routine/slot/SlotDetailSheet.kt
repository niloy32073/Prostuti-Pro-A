package com.prostuti.student.ui.screen.routine.slot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.screen.group.component.GroupConfirmDialog
import com.prostuti.student.ui.screen.routine.RoutineDatePickerDialog
import com.prostuti.student.ui.screen.routine.labelRes
import com.prostuti.student.ui.screen.routine.routineDateText
import com.prostuti.student.ui.screen.routine.topicLabelText
import com.prostuti.student.ui.screen.routine.weekday
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.DhakaTime
import java.time.Instant
import java.time.LocalDate

const val TAG_SLOT_SHEET = "slot_sheet"
const val TAG_SLOT_RESCHEDULE = "slot_reschedule"
const val TAG_SLOT_DELETE = "slot_delete"

/**
 * A slot's details (GROUP_ROUTINE_PLAN.md §6.15): topics, rules, window, my status, and — for whoever
 * may manage it ([canManage]: the group's admin) — "move date" while nobody could have started it,
 * and "delete" for a one-off exam. [lastDate] caps the new date (a routine slot stays inside its routine).
 * The content of the SlotDetail bottom-sheet destination (the scene makes the sheet).
 */
@Composable
fun SlotDetailSheet(
    slot: RoutineSlot,
    label: TopicLabel?,
    now: Instant,
    canManage: Boolean,
    lastDate: LocalDate?,
    isBusy: Boolean,
    onReschedule: (LocalDate) -> Unit,
    onDelete: () -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val today = DhakaTime.dateAt(now.toEpochMilli())
    val date = slot.date

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
            .testTag(TAG_SLOT_SHEET),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val topics = topicLabelText(label ?: TopicLabel.Count(slot.topicIds.size))
        Text(slot.title ?: topics, style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onSurface)
        date?.let {
            Text(
                "${stringResource(it.weekday().labelRes())}, ${routineDateText(it)} · ${slotWindowText(slot)}",
                style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(slotStatusText(slot, slot.phase(now), now), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.slot_detail_topics), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SyllabusList(label, slot.topicIds.size)
        }
        Section(stringResource(R.string.slot_detail_rules), slotConfigText(slot))

        if (canManage && (slot.canReschedule(now) || slot.canDelete(now))) {
            HorizontalDivider()
            if (isBusy) {
                ProstutiSpinner(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (slot.canReschedule(now)) {
                    OutlinedButton(onClick = { picking = true }, modifier = Modifier.testTag(TAG_SLOT_RESCHEDULE)) {
                        Text(stringResource(R.string.slot_reschedule), style = ProstutiTypography.bnLabelLarge)
                    }
                }
                if (slot.canDelete(now)) {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag(TAG_SLOT_DELETE)) {
                        Text(stringResource(R.string.slot_delete_adhoc), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (picking) {
        RoutineDatePickerDialog(
            initial = date ?: today, min = today, max = lastDate,
            onPick = { if (it != date) onReschedule(it) }, onDismiss = { picking = false }
        )
    }
    if (confirmDelete) {
        GroupConfirmDialog(
            title = stringResource(R.string.slot_delete_adhoc_title),
            body = stringResource(R.string.slot_delete_adhoc_body),
            confirmLabel = stringResource(R.string.slot_delete_adhoc),
            destructive = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(body, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
