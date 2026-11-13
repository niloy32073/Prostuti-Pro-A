package com.prostuti.student.ui.screen.routine.builder

import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.prostuti.student.ui.component.paneIsMediumOrWider
import com.prostuti.student.ui.component.paneIsExpandedOrWider
import com.prostuti.student.ui.screen.routine.routineDateText
import com.prostuti.student.ui.screen.routine.minuteOfDayText
import com.prostuti.student.ui.screen.routine.labelRes
import com.prostuti.student.ui.screen.routine.weekday
import com.prostuti.student.ui.screen.routine.topicLabelText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.data.model.response.CurriculumNames
import com.prostuti.student.data.model.response.PlannedSlot
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.DhakaTime

/**
 * Step 4 (routines only): `POST /routines/preview`. The planner's warnings are always shown as the
 * server wrote them (it has no Bangla text, GROUP_ROUTINE_PLAN.md §14 B11), then one row per exam day.
 */
@Composable
internal fun PreviewStep(state: RoutineBuilderUiState, actions: RoutineBuilderActions, modifier: Modifier = Modifier) {
    val plan = state.plan
    val problem = state.previewProblem
    Column(modifier = modifier) {
        when {
            state.isPreviewing -> Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CenteredProgress(Modifier.weight(1f))
                Text(
                    stringResource(R.string.routine_preview_loading), style = ProstutiTypography.bnBodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 32.dp)
                )
            }
            problem != null -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProblemText(problem.message)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { actions.onGoTo(problem.backTo) }) {
                        Text(stringResource(R.string.routine_back_to_step, localizedNumber(problem.backTo.ordinal + 1)))
                    }
                    TextButton(onClick = actions.onRetryPreview) { Text(stringResource(R.string.routine_retry)) }
                }
            }
            plan != null -> {
                val names = state.names
                val warnings: @Composable () -> Unit = {
                    if (plan.warnings.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag(TAG_BUILDER_WARNING)) {
                            Text(stringResource(R.string.routine_preview_warnings), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurface)
                            plan.warnings.forEach { BuilderWarning(it) }
                        }
                    }
                }
                val backToEdit: @Composable () -> Unit = {
                    TextButton(onClick = { actions.onGoTo(BuilderStep.CONFIG) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.routine_back_to_edit), style = ProstutiTypography.bnLabelLarge)
                    }
                }
                val empty: @Composable () -> Unit = {
                    Text(stringResource(R.string.routine_preview_empty), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (paneIsMediumOrWider()) {
                    // Wide: the summary stays put on the left, the exam days fill a grid on the right.
                    Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(0.38f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PreviewSummary(state, plan.examDays)
                            warnings()
                            backToEdit()
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (paneIsExpandedOrWider()) 2 else 1),
                            modifier = Modifier.weight(0.62f).fillMaxHeight().testTag(TAG_BUILDER_PLAN_GRID),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (plan.slots.isEmpty()) item { empty() }
                            items(plan.slots, key = { it.slotDate }) { slot -> PlannedSlotRow(slot, names) }
                        }
                    }
                } else {
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { PreviewSummary(state, plan.examDays) }
                        item { warnings() }
                        if (plan.slots.isEmpty()) item { empty() }
                        items(plan.slots, key = { it.slotDate }) { slot -> PlannedSlotRow(slot, names) }
                        item { backToEdit() }
                    }
                }
            }
            else -> CenteredProgress(Modifier.weight(1f))
        }
        BuilderBottomBar(
            summary = "",
            button = stringResource(R.string.routine_save),
            enabled = plan != null && !state.isPreviewing && plan.slots.isNotEmpty(),
            loading = state.isSaving,
            onClick = actions.onNext
        )
    }
}

@Composable
private fun PreviewSummary(state: RoutineBuilderUiState, examDays: Int) {
    val start = DhakaTime.parseDate(state.form.startDate) ?: state.today
    val end = state.endDate ?: start
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            stringResource(R.string.routine_preview_summary, localizedNumber(examDays), routineDateText(start), routineDateText(end)),
            style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (state.showsWindow) {
            Text(
                stringResource(R.string.routine_preview_window, minuteOfDayText(state.form.windowOpenMinute), minuteOfDayText(state.form.windowCloseMinute)),
                style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PlannedSlotRow(slot: PlannedSlot, names: CurriculumNames) {
    val date = DhakaTime.parseDate(slot.slotDate)
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = date?.let { "${stringResource(it.weekday().labelRes())}, ${routineDateText(it)}" } ?: slot.slotDate,
            style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = topicLabelText(TopicLabel.of(slot.topicGrouping, slot.topicIds, names)),
            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface
        )
    }
}

const val TAG_BUILDER_PLAN_GRID = "builder_plan_grid"
