package com.prostuti.student.ui.screen.routine.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.prostuti.student.ui.component.paneIsExpandedOrWider
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.Weekday
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate

/** Every user action of the builder, so [RoutineBuilderContent] stays stateless and testable. */
class RoutineBuilderActions(
    val onClose: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onGoTo: (BuilderStep) -> Unit = {},
    val onRetryPreview: () -> Unit = {},
    val onToggleSubject: (String) -> Unit = {},
    val onToggleSubjectAll: (String) -> Unit = {},
    val onToggleChapterOpen: (String) -> Unit = {},
    val onToggleChapterAll: (String) -> Unit = {},
    val onToggleTopic: (String) -> Unit = {},
    val onStartDate: (LocalDate) -> Unit = {},
    val onExamDate: (LocalDate) -> Unit = {},
    val onTitle: (String) -> Unit = {},
    val onDurationDays: (Int) -> Unit = {},
    val onWeekday: (Weekday) -> Unit = {},
    val onWindowOpen: (Int) -> Unit = {},
    val onWindowClose: (Int) -> Unit = {},
    val onQuestions: (Int) -> Unit = {},
    val onDurationMinutes: (Int) -> Unit = {},
    val onMark: (Int) -> Unit = {},
    val onNegativeMarking: (Boolean) -> Unit = {},
    val onNegativeMark: (Int) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onClearError: (String) -> Unit = {},
)

/**
 * The routine builder (GROUP_ROUTINE_PLAN.md §6.9–6.10). Back goes one step back, and leaves only
 * from step 1. [onClose] runs once it is saved, or when I turn out not to be the group's admin.
 */
@Composable
fun RoutineBuilderScreen(
    owner: RoutineOwner,
    mode: RoutineBuilderMode,
    onClose: () -> Unit,
    viewModel: RoutineBuilderViewModel = koinViewModel(parameters = { parametersOf(owner, mode) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            viewModel.consumeFinished()
            onClose()
        }
    }
    BackHandler(enabled = state.step != BuilderStep.SUBJECTS) { viewModel.back() }

    RoutineBuilderContent(
        state = state,
        actions = RoutineBuilderActions(
            onClose = onClose,
            onBack = { if (!viewModel.back()) onClose() },
            onNext = viewModel::next,
            onGoTo = viewModel::goTo,
            onRetryPreview = viewModel::retryPreview,
            onToggleSubject = viewModel::toggleSubject,
            onToggleSubjectAll = viewModel::toggleSubjectAll,
            onToggleChapterOpen = viewModel::toggleChapterOpen,
            onToggleChapterAll = viewModel::toggleChapterAll,
            onToggleTopic = viewModel::toggleTopic,
            onStartDate = viewModel::setStartDate,
            onExamDate = viewModel::setExamDate,
            onTitle = viewModel::setTitle,
            onDurationDays = viewModel::changeDurationDays,
            onWeekday = viewModel::toggleWeekday,
            onWindowOpen = viewModel::changeWindowOpen,
            onWindowClose = viewModel::changeWindowClose,
            onQuestions = viewModel::changeQuestions,
            onDurationMinutes = viewModel::changeDurationMinutes,
            onMark = viewModel::changeMark,
            onNegativeMarking = viewModel::setNegativeMarking,
            onNegativeMark = viewModel::changeNegativeMark,
            onRefresh = viewModel::refresh,
            onClearError = viewModel::clearError,
        )
    )
}

@Composable
fun RoutineBuilderContent(state: RoutineBuilderUiState, actions: RoutineBuilderActions) {
    val flowTitle = stringResource(
        when {
            state.isAdhoc -> R.string.routine_builder_title_adhoc
            state.owner is RoutineOwner.Group -> R.string.routine_builder_title_group
            else -> R.string.routine_builder_title_personal
        }
    )
    val stepTitle = stringResource(
        when (state.step) {
            BuilderStep.SUBJECTS -> R.string.routine_step_subjects
            BuilderStep.CHAPTERS -> R.string.routine_step_chapters
            BuilderStep.CONFIG -> R.string.routine_step_config
            BuilderStep.PREVIEW -> R.string.routine_step_preview
        }
    )
    val selecting = state.step == BuilderStep.SUBJECTS || state.step == BuilderStep.CHAPTERS

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = actions.onClearError,
            overlayPadding = padding,
            pullToRefreshEnabled = selecting && !state.isChecking,
            isRefreshing = (state.isLoadingSubjects && state.subjects.isNotEmpty()) || (state.isLoadingChapters && state.groups.isNotEmpty()),
            onRefresh = actions.onRefresh
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                BuilderHeader(
                    title = stepTitle,
                    subtitle = stringResource(
                        R.string.routine_builder_step, flowTitle,
                        localizedNumber(state.step.ordinal + 1), localizedNumber(state.stepCount)
                    ),
                    onBack = actions.onBack
                )
                val step: @Composable (Modifier) -> Unit = { body ->
                    when {
                        state.isChecking -> CenteredProgress(body)
                        state.routineExists && !state.isAdhoc -> RoutineExistsNotice(onClose = actions.onClose, modifier = body)
                        else -> when (state.step) {
                            BuilderStep.SUBJECTS -> SubjectStep(state, actions, body)
                            BuilderStep.CHAPTERS -> ChapterStep(state, actions, body)
                            BuilderStep.CONFIG -> ConfigStep(state, actions, body)
                            BuilderStep.PREVIEW -> PreviewStep(state, actions, body)
                        }
                    }
                }
                if (paneIsExpandedOrWider() && !state.isChecking && !(state.routineExists && !state.isAdhoc)) {
                    // Expanded: every step in a rail on the left, so the whole flow is visible; done steps can be revisited.
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        StepRail(state, onGoTo = actions.onGoTo, modifier = Modifier.width(220.dp).fillMaxHeight())
                        VerticalDivider()
                        step(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    step(Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun RoutineExistsNotice(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            stringResource(R.string.routine_exists_title), style = ProstutiTypography.bnTitleMedium,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.routine_exists_body), style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        PrimaryButton(text = stringResource(R.string.routine_close), onClick = onClose)
    }
}

const val TAG_BUILDER_STEP_RAIL = "builder_step_rail"

/** The builder's steps, top to bottom: done ones can be tapped to go back, later ones are greyed. */
@Composable
private fun StepRail(state: RoutineBuilderUiState, onGoTo: (BuilderStep) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp).testTag(TAG_BUILDER_STEP_RAIL), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BuilderStep.entries.take(state.stepCount).forEach { step ->
            val current = step == state.step
            val done = step.ordinal < state.step.ordinal
            val label = stringResource(
                when (step) {
                    BuilderStep.SUBJECTS -> R.string.routine_step_subjects
                    BuilderStep.CHAPTERS -> R.string.routine_step_chapters
                    BuilderStep.CONFIG -> R.string.routine_step_config
                    BuilderStep.PREVIEW -> R.string.routine_step_preview
                }
            )
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .let { if (done) it.clickable { onGoTo(step) } else it }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .background(if (current || done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    else Text(localizedNumber(step.ordinal + 1), style = ProstutiTypography.bnLabelMedium,
                        color = if (current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(label, style = ProstutiTypography.bnBodyMedium,
                    color = if (current || done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
