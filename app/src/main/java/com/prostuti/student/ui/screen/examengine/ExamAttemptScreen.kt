package com.prostuti.student.ui.screen.examengine

import com.prostuti.student.util.DhakaTime
import com.prostuti.student.ui.screen.routine.minuteOfDayText
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.R
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.screen.examengine.calculator.CalculatorPanel
import com.prostuti.student.ui.screen.examengine.component.McqCard
import com.prostuti.student.ui.screen.examengine.component.McqGridItem
import com.prostuti.student.ui.screen.examengine.component.PassageCard
import com.prostuti.student.ui.screen.examengine.component.TimeUpDialog
import com.prostuti.student.ui.screen.examengine.component.groupQuestionsByPassage
import com.prostuti.student.ui.screen.examengine.scratchpad.ScratchpadPanel
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The shared attempt (question-answering) screen for every exam mode — practice, live and group
 * all finalize through the same submit flow. Once [ExamAttemptViewModel] reports the attempt
 * submitted, [onSubmitted] fires so the caller can move on to the (also mode-common) Result
 * screen; this screen itself never shows a result.
 */
@Composable
fun ExamAttemptScreen(
    attemptId: String,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    /** A group exam's window end (its result stays locked until then); null for every other exam. */
    lockResultUntil: String? = null,
    viewModel: ExamAttemptViewModel = koinViewModel(parameters = { parametersOf(attemptId, lockResultUntil) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.reportResultMessage) {
        val message = state.reportResultMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearReportResultMessage()
        }
    }

    // Live: re-read the clock against start_at on return (ticks stop while in the background).
    LifecycleResumeEffect(Unit) {
        viewModel.resyncTimer()
        onPauseOrDispose { }
    }

    // Leaving a live exam submits it; practice saves the time used. Calls / screen lock don't count.
    val context = LocalContext.current
    val activity = LocalActivity.current
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onBackgrounded(leftApp = isLeavingExam(context, activity))
    }

    // The clock only runs while the exam is on screen — that is what pauses practice time.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val isOnScreen = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    LaunchedEffect(state.timeRemainingSeconds, state.isSubmitted, state.isLoading, isOnScreen) {
        if (isOnScreen && !state.isLoading && state.timeRemainingSeconds > 0 && !state.isSubmitted) {
            delay(1000L)
            viewModel.tickTimer()
        }
    }

    // A time-up submit waits on the dialog below, so the student sees why the exam ended.
    LaunchedEffect(state.isSubmitted, state.isTimeUp) {
        if (state.isSubmitted && !state.isTimeUp) onSubmitted()
    }

    if (state.isTimeUp) {
        TimeUpDialog(
            isSubmitting = state.isSubmitting,
            isSubmitted = state.isSubmitted,
            onViewResult = onSubmitted,
            onRetry = viewModel::submitExam
        )
    } else if (state.showSubmitDialog) {
        SubmitConfirmDialog(
            isSubmitting = state.isSubmitting,
            onConfirm = viewModel::submitExam,
            onDismiss = viewModel::dismissSubmitDialog
        )
    }

    val progress = if (state.totalDurationSeconds > 0) state.timeRemainingSeconds.toFloat() / state.totalDurationSeconds else 0f
    val minutes = state.timeRemainingSeconds / 60
    val seconds = state.timeRemainingSeconds % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    // Right panel state — kept here so it survives tab switches and panel hide/show
    var selectedTab by rememberSaveable { mutableStateOf(0) }      // 0 = calculator, 1 = canvas
    var isPanelVisible by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = viewModel::clearError,
            overlayPadding = padding
        ) {
            val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            state.windowClosesAt?.let { GroupWindowBanner(it) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when {
                    // Expanded: 2-column questions + right panel always visible
                    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            ExamContent(
                                state = state,
                                viewModel = viewModel,
                                progress = progress,
                                timeText = timeText,
                                questionColumns = 2,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            RightPanel(
                                selectedTab = selectedTab,
                                onTabSelect = { selectedTab = it },
                                showHideButton = false,
                                onHide = {},
                                modifier = Modifier
                                    .width(340.dp)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    // Medium: 1-col when panel visible, 2-col when hidden; hide/show toggle
                    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            ExamContent(
                                state = state,
                                viewModel = viewModel,
                                progress = progress,
                                timeText = timeText,
                                questionColumns = if (isPanelVisible) 1 else 2,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            if (isPanelVisible) {
                                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                RightPanel(
                                    selectedTab = selectedTab,
                                    onTabSelect = { selectedTab = it },
                                    showHideButton = true,
                                    onHide = { isPanelVisible = false },
                                    modifier = Modifier
                                        .width(300.dp)
                                        .fillMaxHeight()
                                )
                            } else {
                                // Strip to reveal the panel again
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { isPanelVisible = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "প্যানেল দেখাও",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Compact: single column, no side panel
                    else -> {
                        ExamContent(
                            state = state,
                            viewModel = viewModel,
                            progress = progress,
                            timeText = timeText,
                            questionColumns = 1,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            }
    
        }
    }
}

const val TAG_GROUP_WINDOW_BANNER = "exam_group_window_banner"

/** "গ্রুপ এক্সাম · উইন্ডো শেষ রাত ১১:০০" above a group exam. */
@Composable
private fun GroupWindowBanner(closesAt: String) {
    val millis = DhakaTime.parseInstant(closesAt)?.toEpochMilli() ?: return
    Text(
        text = stringResource(R.string.exam_group_window_banner, minuteOfDayText(DhakaTime.minuteOfDayAt(millis).let { if (it == 0) DhakaTime.MINUTES_PER_DAY else it })),
        style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp).testTag(TAG_GROUP_WINDOW_BANNER)
    )
}

// ── Exam content column (questions + header + submit) ───────────────────────

@Composable
private fun ExamContent(
    state: ExamAttemptUiState,
    viewModel: ExamAttemptViewModel,
    progress: Float,
    timeText: String,
    questionColumns: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable(onClick = viewModel::showSubmitDialog),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "এক্সাম বন্ধ করো",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "${state.selectedAnswers.size}/${state.questions.size} উত্তর দেওয়া হয়েছে",
                style = ProstutiTypography.enLabelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = timeText,
                    style = ProstutiTypography.enLabelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(50.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )

        Spacer(modifier = Modifier.height(8.dp))

        val hPadding = if (questionColumns > 1) 8.dp else 16.dp
        val hSpacing = if (questionColumns > 1) 8.dp else 0.dp
        val gridItems = remember(state.questions, state.passages) {
            groupQuestionsByPassage(state.questions, state.passages)
        }

        if (state.isLoading || state.questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    ProstutiSpinner()
                } else {
                    Text(
                        text = stringResource(R.string.exam_attempt_no_questions),
                        style = ProstutiTypography.bnBodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(questionColumns),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = hPadding, end = hPadding, bottom = 16.dp),
            verticalItemSpacing = 14.dp,
            horizontalArrangement = Arrangement.spacedBy(hSpacing)
        ) {
            items(gridItems, key = { it.key }) { item ->
                when (item) {
                    is McqGridItem.Standalone -> McqCard(
                        index = item.index,
                        question = item.question,
                        selectedOptionId = state.selectedAnswers[item.question.mcqId],
                        onOptionSelected = { optionId -> viewModel.selectAnswer(item.question.mcqId, optionId) },
                        isFavorite = item.question.mcqId in state.favoriteMcqIds,
                        onToggleFavorite = { viewModel.toggleFavorite(item.question.mcqId) },
                        onReport = { reason, description -> viewModel.submitReport(item.question.mcqId, reason, description) },
                        isReported = item.question.mcqId in state.reportedMcqIds
                    )

                    is McqGridItem.PassageGroup -> PassageCard(passage = item.passage) {
                        item.items.forEachIndexed { groupIndex, (questionIndex, question) ->
                            if (groupIndex > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            McqCard(
                                index = questionIndex,
                                question = question,
                                selectedOptionId = state.selectedAnswers[question.mcqId],
                                onOptionSelected = { optionId -> viewModel.selectAnswer(question.mcqId, optionId) },
                                isFavorite = question.mcqId in state.favoriteMcqIds,
                                onToggleFavorite = { viewModel.toggleFavorite(question.mcqId) },
                                onReport = { reason, description -> viewModel.submitReport(question.mcqId, reason, description) },
                                showContainer = false,
                                isReported = question.mcqId in state.reportedMcqIds
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = viewModel::showSubmitDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.qb_submit),
                    style = ProstutiTypography.bnBodyLarge
                )
            }
        }
    }
}

// ── Right panel: tab switcher for calculator / canvas ───────────────────────

@Composable
private fun RightPanel(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    showHideButton: Boolean,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.weight(1f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    text = {
                        Text(
                            text = stringResource(R.string.exam_tool_calculator),
                            style = ProstutiTypography.bnLabelMedium
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    text = {
                        Text(
                            text = stringResource(R.string.exam_tool_canvas),
                            style = ProstutiTypography.bnLabelMedium
                        )
                    }
                )
            }
            if (showHideButton) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onHide)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "◀",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Each tool has its own ViewModel (scoped to this exam's navigation entry), so its state
        // survives tab switches and hiding the panel without touching ExamAttemptViewModel.
        when (selectedTab) {
            0 -> CalculatorPanel(modifier = Modifier.weight(1f).fillMaxWidth())
            else -> ScratchpadPanel(modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

// ── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun SubmitConfirmDialog(isSubmitting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.qb_submit_confirm_title),
                style = ProstutiTypography.bnTitleMedium
            )
        },
        text = {
            Text(
                text = "এক্সাম সাবমিট করলে আর পরিবর্তন করা যাবে না।",
                style = ProstutiTypography.bnBodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isSubmitting, shape = RoundedCornerShape(50.dp)) {
                if (isSubmitting) {
                    ProstutiSpinner(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(text = stringResource(R.string.qb_submit), style = ProstutiTypography.bnBodyMedium)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(text = "বাতিল", style = ProstutiTypography.bnBodyMedium)
            }
        }
    )
}
