package com.prostuti.student.ui.screen.routine.builder

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.VerticalDivider
import com.prostuti.student.ui.component.paneIsMediumOrWider
import com.prostuti.student.ui.screen.routine.RoutineDatePickerDialog
import com.prostuti.student.ui.screen.routine.routineDateText
import com.prostuti.student.ui.screen.routine.minuteOfDayText
import com.prostuti.student.ui.screen.routine.decimalText
import com.prostuti.student.ui.screen.routine.labelRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.Weekday
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.DhakaTime
import java.time.LocalDate

/**
 * Step 3: the routine's days (or the one-off exam's date and name), the daily window (groups
 * only; a personal routine is open all day) and each exam's rules.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConfigStep(state: RoutineBuilderUiState, actions: RoutineBuilderActions, modifier: Modifier = Modifier) {
    val form = state.form
    val problems = state.formProblems
    Column(modifier = modifier) {
        // When and how often (left on wide screens) and the exam's own rules (right).
        val schedule: @Composable ColumnScope.() -> Unit = {
            BuilderWarning(stringResource(if (state.isAdhoc) R.string.routine_lock_warning_adhoc else R.string.routine_lock_warning_routine))

            if (state.isAdhoc) {
                DateField(
                    label = stringResource(R.string.routine_exam_date), date = DhakaTime.parseDate(form.examDate) ?: state.today,
                    today = state.today, onPick = actions.onExamDate, modifier = Modifier.testTag(TAG_BUILDER_EXAM_DATE)
                )
                OutlinedTextField(
                    value = form.title, onValueChange = actions.onTitle, singleLine = true,
                    label = { Text(stringResource(R.string.routine_exam_title)) },
                    placeholder = { Text(stringResource(R.string.routine_exam_title_hint)) },
                    isError = RoutineBuilderForm.Problem.TITLE_TOO_LONG in problems,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag(TAG_BUILDER_TITLE)
                )
                if (RoutineBuilderForm.Problem.TITLE_TOO_LONG in problems) {
                    ProblemText(stringResource(R.string.routine_problem_title_long, localizedNumber(RoutineBuilderForm.MAX_TITLE)))
                }
            } else {
                DateField(
                    label = stringResource(R.string.routine_start_date), date = DhakaTime.parseDate(form.startDate) ?: state.today,
                    today = state.today, onPick = actions.onStartDate, modifier = Modifier.testTag(TAG_BUILDER_START_DATE)
                )
                StepperRow(
                    label = stringResource(R.string.routine_duration_days_label),
                    value = stringResource(R.string.routine_duration_days, localizedNumber(form.durationDays)),
                    onDecrease = { actions.onDurationDays(-1) }, onIncrease = { actions.onDurationDays(1) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag(TAG_BUILDER_WEEKDAYS)) {
                    SectionTitle(stringResource(R.string.routine_weekdays_label))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Weekday.entries.forEach { day ->
                            FilterChip(
                                selected = day in form.weekdays, onClick = { actions.onWeekday(day) },
                                label = { Text(stringResource(day.labelRes()), style = ProstutiTypography.bnBodyMedium) }
                            )
                        }
                    }
                    if (RoutineBuilderForm.Problem.NO_WEEKDAY in problems) ProblemText(stringResource(R.string.routine_problem_no_weekday))
                    Text(stringResource(R.string.routine_weekdays_hint), style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (RoutineBuilderForm.Problem.DATE_IN_PAST in problems) ProblemText(stringResource(R.string.routine_problem_date_in_past))

            if (state.showsWindow) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag(TAG_BUILDER_WINDOW)) {
                    SectionTitle(stringResource(if (state.isAdhoc) R.string.routine_adhoc_window_label else R.string.routine_window_label))
                    StepperRow(
                        label = stringResource(R.string.routine_window_open), value = minuteOfDayText(form.windowOpenMinute),
                        onDecrease = { actions.onWindowOpen(-1) }, onIncrease = { actions.onWindowOpen(1) }
                    )
                    StepperRow(
                        label = stringResource(R.string.routine_window_close), value = minuteOfDayText(form.windowCloseMinute),
                        onDecrease = { actions.onWindowClose(-1) }, onIncrease = { actions.onWindowClose(1) }
                    )
                    if (RoutineBuilderForm.Problem.WINDOW_ORDER in problems) ProblemText(stringResource(R.string.routine_problem_window_order))
                    if (RoutineBuilderForm.Problem.WINDOW_ALREADY_OVER in problems) ProblemText(stringResource(R.string.routine_problem_window_over))
                }
            }

        }
        val rules: @Composable ColumnScope.() -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.routine_exam_config_label))
                StepperRow(
                    label = stringResource(R.string.routine_questions), value = localizedNumber(form.questionsCount),
                    onDecrease = { actions.onQuestions(-1) }, onIncrease = { actions.onQuestions(1) }
                )
                StepperRow(
                    label = stringResource(R.string.routine_duration_minutes), value = localizedNumber(form.durationMinutes),
                    onDecrease = { actions.onDurationMinutes(-1) }, onIncrease = { actions.onDurationMinutes(1) }
                )
                if (RoutineBuilderForm.Problem.DURATION_OVER_WINDOW in problems) {
                    ProblemText(stringResource(R.string.routine_problem_duration_over_window, localizedNumber(form.windowMinutes)))
                }
                StepperRow(
                    label = stringResource(R.string.routine_mark_per_question), value = decimalText(form.markPerQuestion),
                    onDecrease = { actions.onMark(-1) }, onIncrease = { actions.onMark(1) }
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.routine_negative_marking), style = ProstutiTypography.bnBodyMedium,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
                    )
                    Switch(checked = form.negativeMarking, onCheckedChange = actions.onNegativeMarking)
                }
                if (form.negativeMarking) {
                    StepperRow(
                        label = stringResource(R.string.routine_negative_mark), value = decimalText(form.negativeMarkValue),
                        onDecrease = { actions.onNegativeMark(-1) }, onIncrease = { actions.onNegativeMark(1) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.routine_total_marks), style = ProstutiTypography.bnBodyLarge,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
                    )
                    Text(text = decimalText(form.totalMarks), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Gap()
        }
        if (paneIsMediumOrWider()) {
            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp), content = schedule)
                VerticalDivider()
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 4.dp).testTag(TAG_BUILDER_RULES_PANE),
                    verticalArrangement = Arrangement.spacedBy(14.dp), content = rules)
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                schedule()
                HorizontalDivider()
                rules()
            }
        }
        BuilderBottomBar(
            summary = "",
            button = stringResource(if (state.isAdhoc) R.string.routine_schedule_adhoc else R.string.routine_to_preview),
            enabled = state.canLeaveConfig,
            loading = state.isSaving,
            onClick = actions.onNext
        )
    }
}

/** A labelled date button that opens a Material date picker limited to today and later (Dhaka). */
@Composable
private fun DateField(label: String, date: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { open = true }) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text(routineDateText(date), style = ProstutiTypography.bnBodyMedium)
        }
    }
    if (open) {
        RoutineDatePickerDialog(initial = date, min = today, onPick = onPick, onDismiss = { open = false })
    }
}

const val TAG_BUILDER_RULES_PANE = "builder_rules_pane"
