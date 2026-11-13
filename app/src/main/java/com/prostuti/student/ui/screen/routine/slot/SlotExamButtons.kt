package com.prostuti.student.ui.screen.routine.slot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.theme.ProstutiTypography
import androidx.lifecycle.compose.dropUnlessResumed

/** What a slot's exam button asks for: start (or finish, or practise) the exam, see my result, or the ranking. */
enum class SlotAction { TAKE, VIEW_RESULT, RANKING }

const val TAG_SLOT_ACTION_PREFIX = "slot_action_"

/**
 * The exam button(s) for a slot's phase (GROUP_ROUTINE_PLAN.md §6.5, §6.12):
 * open → "take the exam"; in progress → "finish the exam" (D9: reopening submits it);
 * submitted → "view" (locked result); ended → "ranking and answer sheet" + "take as practice";
 * missed → the ranking too, when the exam exists ([hasExam]), + "take as practice".
 * Nothing before the window opens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlotExamButtons(phase: SlotPhase, isBusy: Boolean, slotId: String, hasExam: Boolean, onAction: (SlotAction) -> Unit) {
    if (phase == SlotPhase.UPCOMING) return
    if (isBusy) {
        ProstutiSpinner(modifier = Modifier.padding(top = 6.dp).size(24.dp))
        return
    }
    val tag = Modifier.testTag(TAG_SLOT_ACTION_PREFIX + slotId)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        when (phase) {
            SlotPhase.OPEN -> Button(onClick = { onAction(SlotAction.TAKE) }, modifier = tag) { Label(R.string.slot_action_take) }
            SlotPhase.IN_PROGRESS -> Button(onClick = { onAction(SlotAction.TAKE) }, modifier = tag) { Label(R.string.slot_action_finish) }
            SlotPhase.SUBMITTED -> OutlinedButton(onClick = { onAction(SlotAction.VIEW_RESULT) }, modifier = tag) { Label(R.string.slot_action_view_submitted) }
            SlotPhase.ENDED, SlotPhase.MISSED -> {
                if (hasExam) OutlinedButton(onClick = dropUnlessResumed { onAction(SlotAction.RANKING) }, modifier = tag) { Label(R.string.slot_action_ranking) }
                TextButton(onClick = { onAction(SlotAction.TAKE) }, modifier = if (hasExam) Modifier else tag) { Label(R.string.slot_action_practice) }
            }
            SlotPhase.UPCOMING -> Unit
        }
    }
}

@Composable
private fun Label(res: Int) = Text(stringResource(res), style = ProstutiTypography.bnLabelLarge)
