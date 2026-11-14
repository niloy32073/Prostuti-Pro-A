package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.VerticalDivider
import com.prostuti.student.ui.component.paneIsMediumOrWider
import com.prostuti.student.ui.component.paneIsExpandedOrWider
import com.prostuti.student.ui.screen.routine.WeekDayList
import com.prostuti.student.ui.screen.routine.WeekExams
import com.prostuti.student.ui.screen.routine.slot.PrepareCard
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.prostuti.student.ui.screen.routine.TAG_WEEK_DAY_PREFIX
import com.prostuti.student.ui.screen.routine.WeekStrip
import com.prostuti.student.ui.screen.routine.WeekStripState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.InfoNote
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.screen.group.component.GroupConfirmDialog
import com.prostuti.student.ui.screen.routine.labelRes
import com.prostuti.student.ui.screen.routine.routineDateText
import com.prostuti.student.ui.screen.routine.slot.RefreshAtWindowEdges
import com.prostuti.student.ui.screen.routine.slot.HandleExamLaunch
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import com.prostuti.student.ui.screen.routine.slot.SlotCard
import com.prostuti.student.ui.screen.routine.slot.date
import com.prostuti.student.ui.screen.routine.slot.rememberNow
import com.prostuti.student.ui.screen.routine.topicLabelText
import com.prostuti.student.ui.screen.routine.weekday
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.LocalDate

const val TAG_CAL_WEEK_EXAMS = "calendar_week_exams"
const val TAG_CAL_DAY_PREFIX = TAG_WEEK_DAY_PREFIX
const val TAG_CAL_MENU = "calendar_menu"
const val TAG_CAL_DAY_TEXT = "calendar_day_text"

@Composable
fun GroupRoutineCalendarScreen(
    groupId: String,
    onBack: () -> Unit,
    onScheduleAdhoc: () -> Unit,
    onGroupClosed: () -> Unit,
    onOpenExamAttempt: (attemptId: String, title: String, lockResultUntil: String?) -> Unit,
    onOpenExamResult: (attemptId: String, title: String, lockResultUntil: String?) -> Unit,
    onOpenRanking: (examId: String, title: String, closesAt: String) -> Unit,
    /** A slot was opened: show its sheet (SlotDetail, which shares this ViewModel). */
    onShowSlot: () -> Unit = {},
    viewModel: GroupRoutineCalendarViewModel = koinViewModel(parameters = { parametersOf(groupId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // A slot was opened: its sheet is a destination (SlotDetail) that reads this same ViewModel.
    LaunchedEffect(state.openSlotId) { if (state.openSlotId != null) onShowSlot() }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Back on screen (another tab, scheduling, an exam): quietly show what changed. The first
    // open reuses the ViewModel's own load.
    LaunchedEffect(Unit) { viewModel.onReturn() }
    RefreshAtWindowEdges(slots = state.weekSlots, onRefresh = viewModel::refresh)
    // Exam titles for the engine's screens: the slot's name, else its topics.
    val titles = state.weekSlots.associate { it.id to (it.title ?: topicLabelText(state.labels[it.id] ?: TopicLabel.Count(it.topicIds.size))) }
    HandleExamLaunch(
        launch = state.examLaunch, titleOf = { titles[it].orEmpty() }, onConsumed = viewModel::consumeExamLaunch,
        onOpenAttempt = onOpenExamAttempt, onOpenResult = onOpenExamResult,
    )

    LaunchedEffect(state.notice) {
        state.notice?.let {
            viewModel.consumeNotice()
            snackbarHostState.showSnackbar(context.getString(it.res, it.arg))
        }
    }
    LaunchedEffect(state.isGone) {
        if (state.isGone) {
            viewModel.consumeGone()
            onGroupClosed()
        }
    }

    GroupRoutineCalendarContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onShowWeek = viewModel::showWeek,
        onSelectDate = viewModel::selectDate,
        onOpenSlot = viewModel::openSlot,
        onSlotAction = { slotId, action ->
            val slot = state.weekSlots.firstOrNull { it.id == slotId }
            val examId = slot?.examId
            if (action == SlotAction.RANKING) { if (examId != null) onOpenRanking(examId, titles[slotId].orEmpty(), slot.closesAt) }
            else viewModel.openExam(slotId, action)
        },
        onScheduleAdhoc = onScheduleAdhoc,
        onDeleteRoutine = viewModel::deleteRoutine,
        onRefresh = viewModel::pullToRefresh,
        onClearError = viewModel::clearError,
    )
}

@Composable
fun GroupRoutineCalendarContent(
    state: GroupCalendarUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onShowWeek: (forward: Boolean) -> Unit = {},
    onSelectDate: (LocalDate) -> Unit = {},
    onOpenSlot: (String) -> Unit = {},
    onSlotAction: (String, SlotAction) -> Unit = { _, _ -> },
    onScheduleAdhoc: () -> Unit = {},
    onDeleteRoutine: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onClearError: (String) -> Unit = {},
    /** Fixed time for tests; null = the ticking clock. */
    clock: Instant? = null,
) {
    val ticking by rememberNow()
    val now = clock ?: ticking
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.feature_coming_soon)
    var confirmDeleteRoutine by rememberSaveable { mutableStateOf(false) }

    if (confirmDeleteRoutine) {
        GroupConfirmDialog(
            title = stringResource(R.string.calendar_delete_routine_title),
            body = stringResource(R.string.calendar_delete_routine_body),
            confirmLabel = stringResource(R.string.calendar_delete_routine),
            destructive = true,
            onConfirm = { confirmDeleteRoutine = false; onDeleteRoutine() },
            onDismiss = { confirmDeleteRoutine = false },
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = onClearError,
            overlayPadding = padding,
            pullToRefreshEnabled = true,
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                CalendarHeader(
                    groupName = state.group?.name, isAdmin = state.isAdmin, hasRoutine = state.routine != null,
                    busy = state.isDeletingRoutine, onBack = onBack,
                    onScheduleAdhoc = dropUnlessResumed { onScheduleAdhoc() }, onDeleteRoutine = { confirmDeleteRoutine = true }
                )
                if (state.group == null && state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProstutiSpinner()
                    }
                    return@Column
                }
                val strip = WeekStripState(state.weekStart, state.selectedDate, state.today, state.canGoBack, state.canGoForward) { state.markOf(it, now) }
                val period: @Composable () -> Unit = {
                    val start = state.routineStart
                    val end = state.routineEnd
                    if (start != null && end != null) {
                        Text(
                            stringResource(R.string.calendar_routine_period, routineDateText(start), routineDateText(end)),
                            style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val day: @Composable () -> Unit = {
                    DayContent(state, now, onOpenSlot, onSlotAction) { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }
                }
                val wide = paneIsMediumOrWider()
                val extraWide = paneIsExpandedOrWider()
                if (!wide) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        period()
                        WeekStrip(strip, onShowWeek, onSelectDate)
                        day()
                        if (state.routine != null) InfoNote(stringResource(R.string.calendar_note))
                        Spacer(Modifier.size(8.dp))
                    }
                } else {
                    // Medium: the week's days beside the chosen day. Expanded: plus the week's exams at a glance.
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(if (extraWide) 0.26f else 0.38f).fillMaxHeight().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            period()
                            WeekDayList(strip, onShowWeek, onSelectDate)
                        }
                        VerticalDivider()
                        Column(Modifier.weight(if (extraWide) 0.44f else 0.62f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            day()
                            if (!extraWide && state.routine != null) InfoNote(stringResource(R.string.calendar_note))
                            Spacer(Modifier.size(8.dp))
                        }
                        if (extraWide) {
                            VerticalDivider()
                            Column(Modifier.weight(0.3f).fillMaxHeight().verticalScroll(rememberScrollState()).testTag(TAG_CAL_WEEK_EXAMS),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                WeekExams(state.weekSlots, state.labels, onSelectDate)
                                if (state.routine != null) InfoNote(stringResource(R.string.calendar_note))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    groupName: String?, isAdmin: Boolean, hasRoutine: Boolean, busy: Boolean,
    onBack: () -> Unit, onScheduleAdhoc: () -> Unit, onDeleteRoutine: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(onClick = onBack)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.calendar_title), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
            groupName?.let {
                Text(it, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (busy) ProstutiSpinner(modifier = Modifier.size(22.dp))
        if (isAdmin) {
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag(TAG_CAL_MENU)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.calendar_menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.group_schedule_adhoc)) }, onClick = { menu = false; onScheduleAdhoc() })
                    if (hasRoutine) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.calendar_delete_routine), color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDeleteRoutine() }
                        )
                    }
                }
            }
        }
    }
}

/** The chosen day: its exams, or (D8) the next exam's date and topics to get ready for. */
@Composable
private fun DayContent(state: GroupCalendarUiState, now: Instant, onOpenSlot: (String) -> Unit, onSlotAction: (String, SlotAction) -> Unit, onConcepts: () -> Unit) {
    val date = state.selectedDate
    val slots = state.selectedSlots
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "${stringResource(date.weekday().labelRes())}, ${routineDateText(date)}",
            style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface
        )
        if (slots.isNotEmpty()) {
            slots.forEach {
                SlotCard(
                    slot = it, label = state.labels[it.id], now = now, onClick = { onOpenSlot(it.id) },
                    isBusy = state.busyExamSlotId == it.id, onAction = { action -> onSlotAction(it.id, action) },
                )
            }
            return@Column
        }
        val next = state.nextExamAfter(date)
        val text = when {
            state.isEmpty -> stringResource(R.string.calendar_empty)
            state.routine != null && !state.isInRoutine(date) && next == null -> stringResource(R.string.calendar_outside_routine)
            next != null -> stringResource(R.string.calendar_no_exam)
            else -> stringResource(R.string.calendar_no_exam)
        }
        Text(
            text, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
                .padding(16.dp).testTag(TAG_CAL_DAY_TEXT)
        )
        // D8, for every member: what to study for the next exam.
        if (next != null) PrepareCard(next, state.labels[next.id], onConcepts)
    }
}
