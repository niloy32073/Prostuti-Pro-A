package com.prostuti.student.ui.screen.routine

import com.prostuti.student.ui.screen.routine.slot.PrepareCard
import com.prostuti.student.ui.screen.FeatureAccessViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.component.InfoNote
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.screen.group.component.GroupConfirmDialog
import com.prostuti.student.ui.screen.routine.slot.HandleExamLaunch
import com.prostuti.student.ui.screen.routine.slot.RefreshAtWindowEdges
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import com.prostuti.student.ui.screen.routine.slot.SlotCard
import com.prostuti.student.ui.screen.routine.slot.SlotDetailSheet
import com.prostuti.student.ui.screen.routine.slot.date
import com.prostuti.student.ui.screen.routine.slot.rememberNow
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.LocalDate

const val TAG_ROUTINE_MODE_PERSONAL = "routine_mode_personal"
const val TAG_ROUTINE_MODE_GROUP = "routine_mode_group"
const val TAG_ROUTINE_CREATE = "routine_create"
const val TAG_ROUTINE_NO_GROUPS = "routine_no_groups"
const val TAG_ROUTINE_FEATURE_OFF = "routine_feature_off"
const val TAG_ROUTINE_DAY_TEXT = "routine_day_text"
const val TAG_ROUTINE_MENU = "routine_menu"
const val TAG_ROUTINE_GROUP_FILTER_PREFIX = "routine_group_filter_"
const val TAG_ROUTINE_WEEK_OVERVIEW = "routine_week_overview"

@Composable
fun RoutineTabScreen(
    onCreatePersonalRoutine: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenExamAttempt: (attemptId: String, title: String, lockResultUntil: String?) -> Unit,
    onOpenExamResult: (attemptId: String, title: String, lockResultUntil: String?) -> Unit,
    onOpenRanking: (groupId: String, examId: String, title: String, closesAt: String) -> Unit,
    viewModel: RoutineTabViewModel = koinViewModel(),
    accessViewModel: FeatureAccessViewModel = koinViewModel(),
) {
    val access by accessViewModel.access.collectAsStateWithLifecycle()
    if (!access.routine) {
        RoutineFeatureOff()
        return
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Back on screen (another tab, the builder, an exam): quietly show what changed. The first
    // open reuses the ViewModel's own load.
    LaunchedEffect(Unit) { viewModel.onReturn() }
    RefreshAtWindowEdges(slots = state.weekSlots, onRefresh = viewModel::refresh)
    LaunchedEffect(state.notice) {
        state.notice?.let {
            viewModel.consumeNotice()
            snackbarHostState.showSnackbar(context.getString(it.res, it.arg))
        }
    }
    val titles = state.weekSlots.associate { it.id to (it.title ?: topicLabelText(state.labels[it.id] ?: TopicLabel.Count(it.topicIds.size))) }
    HandleExamLaunch(
        launch = state.examLaunch, titleOf = { titles[it].orEmpty() }, onConsumed = viewModel::consumeExamLaunch,
        onOpenAttempt = onOpenExamAttempt, onOpenResult = onOpenExamResult,
    )

    RoutineTabContent(
        state = state,
        snackbarHostState = snackbarHostState,
        actions = RoutineTabActions(
            onSelectMode = viewModel::selectMode,
            onSelectGroup = viewModel::selectGroup,
            onShowWeek = viewModel::showWeek,
            onSelectDate = viewModel::selectDate,
            onOpenSlot = viewModel::openSlot,
            onDismissSlot = viewModel::dismissSlot,
            onRescheduleSlot = viewModel::reschedule,
            onDeleteSlot = viewModel::deleteSlot,
            onSlotAction = { slotId, action ->
                val slot = state.weekSlots.firstOrNull { it.id == slotId }
                val groupId = slot?.groupId
                val examId = slot?.examId
                when {
                    action != SlotAction.RANKING -> viewModel.openExam(slotId, action)
                    // A personal exam has no ranking: its button shows my result instead.
                    groupId == null -> viewModel.openExam(slotId, SlotAction.VIEW_RESULT)
                    examId != null -> onOpenRanking(groupId, examId, titles[slotId].orEmpty(), slot.closesAt)
                }
            },
            onCreateRoutine = onCreatePersonalRoutine,
            onDeleteRoutine = viewModel::deleteRoutine,
            onOpenGroups = onOpenGroups,
            onRefresh = viewModel::pullToRefresh,
            onClearError = viewModel::clearError,
        ),
    )
}

/** Everything the user can do on the tab, so [RoutineTabContent] stays stateless and testable. */
class RoutineTabActions(
    val onSelectMode: (RoutineTabMode) -> Unit = {},
    val onSelectGroup: (String?) -> Unit = {},
    val onShowWeek: (Boolean) -> Unit = {},
    val onSelectDate: (LocalDate) -> Unit = {},
    val onOpenSlot: (String) -> Unit = {},
    val onDismissSlot: () -> Unit = {},
    val onRescheduleSlot: (LocalDate) -> Unit = {},
    val onDeleteSlot: () -> Unit = {},
    val onSlotAction: (String, SlotAction) -> Unit = { _, _ -> },
    val onCreateRoutine: () -> Unit = {},
    val onDeleteRoutine: () -> Unit = {},
    val onOpenGroups: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onClearError: (String) -> Unit = {},
)

/**
 * Design Routine / RoutineMedium / RoutineExpanded (GROUP_ROUTINE_PLAN.md §6.16): compact stacks the
 * week strip over the day; medium puts a day list beside the day; expanded adds the week's exams.
 */
@Composable
fun RoutineTabContent(
    state: RoutineTabUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    actions: RoutineTabActions = RoutineTabActions(),
    /** Fixed time for tests; null = the ticking clock. */
    clock: Instant? = null,
) {
    val ticking by rememberNow()
    val now = clock ?: ticking
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.feature_coming_soon)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    state.openSlot?.let { slot ->
        SlotDetailSheet(
            slot = slot, label = state.labels[slot.id], now = now, canManage = state.canManage(slot),
            lastDate = if (slot.groupId == null) state.routineEnd else null, isBusy = state.isSlotBusy,
            onReschedule = actions.onRescheduleSlot, onDelete = actions.onDeleteSlot, onDismiss = actions.onDismissSlot,
        )
    }
    if (confirmDelete) {
        GroupConfirmDialog(
            title = stringResource(R.string.calendar_delete_routine_title), body = stringResource(R.string.calendar_delete_routine_body),
            confirmLabel = stringResource(R.string.calendar_delete_routine), destructive = true,
            onConfirm = { confirmDelete = false; actions.onDeleteRoutine() }, onDismiss = { confirmDelete = false },
        )
    }

    val strip = WeekStripState(state.weekStart, state.selectedDate, state.today, state.canGoBack, state.canGoForward) {
        dayMark(state.slotsOn(it), now)
    }
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val panes = when {
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> 3
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> 2
        else -> 1
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors, errorCleaner = actions.onClearError, overlayPadding = padding,
            pullToRefreshEnabled = true, isRefreshing = state.isRefreshing, onRefresh = actions.onRefresh,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                RoutineHeader(state, busy = state.isDeletingRoutine, onDelete = { confirmDelete = true })
                ModeToggle(state.mode, actions.onSelectMode)
                if (!state.isPersonal && state.groups.size > 1) GroupFilter(state, actions.onSelectGroup)

                val body = Modifier.fillMaxWidth().weight(1f)
                when {
                    state.isPersonal && !state.routineKnown || !state.isPersonal && !state.groupsKnown -> Box(body, contentAlignment = Alignment.Center) {
                        ProstutiSpinner()
                    }
                    state.isPersonal && state.routine == null -> EmptyPersonal(actions.onCreateRoutine, body)
                    !state.isPersonal && state.groups.isEmpty() -> NoGroups(actions.onOpenGroups, body)
                    panes == 1 -> Column(body.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        WeekStrip(strip, actions.onShowWeek, actions.onSelectDate)
                        DayPlan(state, now, actions) { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }
                        Spacer(Modifier.size(8.dp))
                    }
                    else -> Row(body.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        WeekDayList(strip, actions.onShowWeek, actions.onSelectDate, Modifier.weight(if (panes == 3) 0.28f else 0.4f).verticalScroll(rememberScrollState()))
                        VerticalDivider()
                        Column(Modifier.weight(if (panes == 3) 0.42f else 0.6f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            DayPlan(state, now, actions) { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }
                        }
                        if (panes == 3) {
                            VerticalDivider()
                            WeekExams(state.weekSlots, state.labels, actions.onSelectDate, Modifier.weight(0.3f).fillMaxHeight().verticalScroll(rememberScrollState()).testTag(TAG_ROUTINE_WEEK_OVERVIEW))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineHeader(state: RoutineTabUiState, busy: Boolean, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.routine_tab_title), style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onBackground)
            val routine = state.routine
            val left = state.daysLeft
            if (state.isPersonal && routine != null && left != null) {
                Text(
                    stringResource(R.string.routine_tab_plan, localizedNumber(routine.durationDays), localizedNumber(left.toInt())),
                    style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (busy) ProstutiSpinner(Modifier.size(22.dp))
        if (state.isPersonal && state.routine != null) {
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.testTag(TAG_ROUTINE_MENU)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.calendar_menu))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.routine_tab_delete_new), color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(mode: RoutineTabMode, onSelect: (RoutineTabMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        listOf(RoutineTabMode.PERSONAL to R.string.routine_tab_personal, RoutineTabMode.GROUP to R.string.routine_tab_group).forEachIndexed { i, (m, label) ->
            SegmentedButton(
                selected = mode == m, onClick = { onSelect(m) }, shape = SegmentedButtonDefaults.itemShape(i, 2),
                modifier = Modifier.testTag(if (m == RoutineTabMode.PERSONAL) TAG_ROUTINE_MODE_PERSONAL else TAG_ROUTINE_MODE_GROUP),
            ) { Text(stringResource(label)) }
        }
    }
}

@Composable
private fun GroupFilter(state: RoutineTabUiState, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(selected = state.groupFilter == null, onClick = { onSelect(null) }, label = { Text(stringResource(R.string.routine_tab_all_groups)) })
        state.groups.forEach { g ->
            FilterChip(
                selected = state.groupFilter == g.id, onClick = { onSelect(g.id) }, label = { Text(g.name) },
                modifier = Modifier.testTag(TAG_ROUTINE_GROUP_FILTER_PREFIX + g.id)
            )
        }
    }
}

/** The chosen day: its exams; else (D8) the next exam and its topics to get ready for; "read concepts". */
@Composable
private fun DayPlan(state: RoutineTabUiState, now: Instant, actions: RoutineTabActions, onConcepts: () -> Unit) {
    val date = state.selectedDate
    Text(
        stringResource(R.string.routine_tab_day_title, "${stringResource(date.weekday().labelRes())}, ${routineDateText(date)}"),
        style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface
    )
    val slots = state.slotsOn(date)
    if (slots.isNotEmpty()) {
        slots.forEach { slot ->
            SlotCard(
                slot = slot, label = state.labels[slot.id], now = now, onClick = { actions.onOpenSlot(slot.id) },
                groupName = slot.groupId?.let { state.groupNames[it] }, isBusy = state.busyExamSlotId == slot.id,
                onAction = { actions.onSlotAction(slot.id, it) },
            )
        }
    } else {
        val next = state.nextExamAfter(date)
        val outside = state.isPersonal && !state.isInRoutine(date)
        val text = when {
            outside -> stringResource(R.string.calendar_outside_routine)
            next != null -> stringResource(R.string.calendar_no_exam)
            state.isPersonal -> stringResource(R.string.calendar_no_exam)
            else -> stringResource(R.string.routine_tab_group_none_day)
        }
        Text(
            text, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
                .padding(16.dp).testTag(TAG_ROUTINE_DAY_TEXT)
        )
        // D8: what to study for the next exam; concepts aren't in the app yet, so that button says "coming soon".
        if (next != null && !outside) {
            PrepareCard(next, state.labels[next.id], onConcepts, groupName = next.groupId?.let { state.groupNames[it] })
        }
    }
    if (!state.isPersonal) InfoNote(stringResource(R.string.calendar_note))
}

/** Expanded only: every exam of the shown week; tapping one picks its day. */
@Composable
private fun EmptyPersonal(onCreate: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Text(stringResource(R.string.routine_tab_empty), style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        PrimaryButton(text = stringResource(R.string.routine_tab_create), onClick = dropUnlessResumed { onCreate() }, modifier = Modifier.testTag(TAG_ROUTINE_CREATE))
    }
}

@Composable
private fun NoGroups(onOpenGroups: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(24.dp).testTag(TAG_ROUTINE_NO_GROUPS), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Text(stringResource(R.string.routine_tab_no_groups), style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton(text = stringResource(R.string.routine_tab_go_groups), onClick = dropUnlessResumed { onOpenGroups() })
    }
}

/** Shown instead of the tab when the account has no `routine:use` scope (§6.20). */
@Composable
fun RoutineFeatureOff(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp).testTag(TAG_ROUTINE_FEATURE_OFF), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.routine_feature_off), style = ProstutiTypography.bnBodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
