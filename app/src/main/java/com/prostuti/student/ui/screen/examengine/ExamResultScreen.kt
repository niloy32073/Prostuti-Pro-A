package com.prostuti.student.ui.screen.examengine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.domain.parseServerTimeMillis
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.theme.ProstutiTypography
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.prostuti.student.R
import com.prostuti.student.ui.component.appLanguage
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.util.LANGUAGE_BANGLA
import com.prostuti.student.util.toBengaliDigits
import java.util.Locale
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExamResultScreen(
    attemptId: String,
    titleBn: String,
    onViewAnswerSheet: () -> Unit,
    onViewAnalysis: () -> Unit,
    onViewLeaderboard: (examId: String) -> Unit,
    onFinish: () -> Unit,
    fromHistory: Boolean = false,
    onRetakeStarted: (ExamAttemptData) -> Unit = {},
    /** True when this screen is the list pane of a list-detail scene (medium and wider windows). */
    showPaneList: Boolean = false,
    /** The screen open in the pane beside this one; highlighted in the list. */
    selectedPane: ResultPane? = null,
    viewModel: ExamResultViewModel = koinViewModel(parameters = { parametersOf(attemptId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.retakeAttempt) {
        state.retakeAttempt?.let { attempt ->
            viewModel.onRetakeConsumed()
            onRetakeStarted(attempt)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = viewModel::clearError,
            overlayPadding = padding
        ) {
            if (state.isLoading && state.result == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ProstutiSpinner()
                }
            } else {
                state.result?.let { result ->
                    ResultContent(
                        titleBn = titleBn,
                        result = result,
                        fromHistory = fromHistory,
                        isStartingRetake = state.isStartingRetake,
                        onViewAnswerSheet = onViewAnswerSheet,
                        onViewAnalysis = onViewAnalysis,
                        onViewLeaderboard = { onViewLeaderboard(result.examId) },
                        onRetake = viewModel::retake,
                        onFinish = onFinish,
                        showPaneList = showPaneList,
                        selectedPane = selectedPane,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                }
            }
    
        }
    }
}

@Composable
private fun ResultContent(
    titleBn: String,
    result: ExamAttemptResultResponse,
    fromHistory: Boolean,
    isStartingRetake: Boolean,
    onViewAnswerSheet: () -> Unit,
    onViewAnalysis: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onRetake: () -> Unit,
    onFinish: () -> Unit,
    showPaneList: Boolean,
    selectedPane: ResultPane?,
    modifier: Modifier = Modifier
) {
    val totalQuestions = result.rightAnswerCount + result.wrongAnswerCount + result.skipCount
    val scorePercent = if (totalQuestions > 0) (result.rightAnswerCount * 100f / totalQuestions) else 0f
    val wrongPercent = if (totalQuestions > 0) (result.wrongAnswerCount * 100f / totalQuestions) else 0f
    val timeTakenText = remember(result.startAt, result.endAt) { formatTimeTaken(result.startAt, result.endAt) }

    // In a pane the whole column scrolls (the pane can be short); full screen keeps the buttons pinned at the bottom.
    Column(
        modifier = if (showPaneList) modifier.verticalScroll(rememberScrollState()) else modifier,
    ) {
        Column(
            modifier = Modifier
                .then(if (showPaneList) Modifier else Modifier.weight(1f))
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            if (fromHistory) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onFinish)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.result_title),
                            style = ProstutiTypography.bnTitleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = titleBn,
                            style = ProstutiTypography.bnLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.result_finished_title),
                    style = ProstutiTypography.bnHeadlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = titleBn,
                    style = ProstutiTypography.bnBodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            ScoreDonut(
                correctPercent = scorePercent,
                wrongPercent = wrongPercent,
                rightCount = result.rightAnswerCount,
                total = totalQuestions
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultStatTile(
                    value = localizedNumber(result.rightAnswerCount),
                    label = stringResource(R.string.result_correct),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                ResultStatTile(
                    value = localizedNumber(result.wrongAnswerCount),
                    label = stringResource(R.string.result_wrong),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                ResultStatTile(
                    value = localizedNumber(result.skipCount),
                    label = stringResource(R.string.result_skipped),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResultSummaryRow(label = stringResource(R.string.result_time_taken), value = timeTakenText.localizedDigits())
                ResultSummaryRow(
                    label = stringResource(R.string.result_final_mark),
                    value = "%.2f".format(Locale.ROOT, result.totalScore).localizedDigits(),
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                if (result.mode == AttemptMode.LIVE && result.rank != null) {
                    ResultSummaryRow(label = stringResource(R.string.result_rank), value = "#${result.rank}".localizedDigits())
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showPaneList) {
                ResultPaneList(
                    isLive = result.mode == AttemptMode.LIVE,
                    selected = selectedPane,
                    onViewAnswerSheet = onViewAnswerSheet,
                    onViewAnalysis = onViewAnalysis,
                    onViewLeaderboard = onViewLeaderboard
                )
            } else {
                OutlinedButton(
                    onClick = dropUnlessResumed { onViewAnswerSheet() },
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(text = stringResource(R.string.result_view_answer_sheet), style = ProstutiTypography.bnBodyLarge)
                }
                OutlinedButton(
                    onClick = dropUnlessResumed { onViewAnalysis() },
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag(RESULT_ANALYSIS_BUTTON_TAG)
                ) {
                    Text(text = stringResource(R.string.result_view_analysis), style = ProstutiTypography.bnBodyLarge)
                }
                if (result.mode == AttemptMode.LIVE) {
                    OutlinedButton(
                        onClick = dropUnlessResumed { onViewLeaderboard() },
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(text = stringResource(R.string.result_view_leaderboard), style = ProstutiTypography.bnBodyLarge)
                    }
                }
            }
            if (fromHistory) {
                Button(
                    onClick = onRetake,
                    enabled = !isStartingRetake,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag(RESULT_RETAKE_BUTTON_TAG)
                ) {
                    if (isStartingRetake) {
                        ProstutiSpinner(modifier = Modifier.size(20.dp))
                    } else {
                        Text(text = stringResource(R.string.result_retake), style = ProstutiTypography.bnBodyLarge)
                    }
                }
                Text(
                    text = stringResource(R.string.result_retake_note),
                    style = ProstutiTypography.bnLabelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(text = stringResource(R.string.result_go_home), style = ProstutiTypography.bnBodyLarge)
                }
            }
        }
    }
}

/**
 * The result's three views as one list card, instead of three full-width buttons: used when the
 * picked view opens in the pane beside the result. The open one is highlighted.
 */
@Composable
private fun ResultPaneList(
    isLive: Boolean,
    selected: ResultPane?,
    onViewAnswerSheet: () -> Unit,
    onViewAnalysis: () -> Unit,
    onViewLeaderboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .selectableGroup()
    ) {
        ResultPaneRow(
            title = stringResource(R.string.result_pane_answer_sheet),
            subtitle = stringResource(R.string.result_pane_answer_sheet_sub),
            icon = Icons.Outlined.Quiz,
            selected = selected == ResultPane.ANSWER_SHEET,
            onClick = dropUnlessResumed { onViewAnswerSheet() },
            modifier = Modifier.testTag(RESULT_ANSWER_SHEET_ROW_TAG)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ResultPaneRow(
            title = stringResource(R.string.result_pane_analysis),
            subtitle = stringResource(R.string.result_pane_analysis_sub),
            icon = Icons.Outlined.Insights,
            selected = selected == ResultPane.ANALYSIS,
            onClick = dropUnlessResumed { onViewAnalysis() },
            modifier = Modifier.testTag(RESULT_ANALYSIS_BUTTON_TAG)
        )
        if (isLive) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ResultPaneRow(
                title = stringResource(R.string.result_pane_leaderboard),
                subtitle = stringResource(R.string.result_pane_leaderboard_sub),
                icon = Icons.Outlined.Leaderboard,
                selected = selected == ResultPane.LEADERBOARD,
                onClick = dropUnlessResumed { onViewLeaderboard() },
                modifier = Modifier.testTag(RESULT_LEADERBOARD_ROW_TAG)
            )
        }
    }
}

@Composable
private fun ResultPaneRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container)
            .selectable(selected = selected, role = Role.Tab, onClick = dropUnlessResumed { onClick() })
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = ProstutiTypography.bnBodyLarge, color = content)
            Text(text = subtitle, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScoreDonut(
    correctPercent: Float,
    wrongPercent: Float,
    rightCount: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val success = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    val brush = remember(correctPercent, wrongPercent, success, error, track) {
        val correctStop = (correctPercent / 100f).coerceIn(0f, 1f)
        val wrongStop = ((correctPercent + wrongPercent) / 100f).coerceIn(0f, 1f)
        Brush.sweepGradient(
            0f to success,
            correctStop to success,
            correctStop to error,
            wrongStop to error,
            wrongStop to track,
            1f to track
        )
    }

    Box(
        modifier = modifier.size(176.dp).clip(CircleShape).background(brush),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$rightCount/$total".localizedDigits(),
                    style = ProstutiTypography.enHeadlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.result_score_percent, localizedNumber(correctPercent.toInt())),
                    style = ProstutiTypography.enLabelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultStatTile(
    value: String,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = ProstutiTypography.enTitleLarge, color = contentColor)
        Text(
            text = label,
            style = ProstutiTypography.bnLabelMedium,
            color = contentColor,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ResultSummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = ProstutiTypography.enLabelLarge, color = valueColor)
    }
}

/** In Bangla, Latin digits become Bengali ones; English keeps them. */
@Composable
@ReadOnlyComposable
private fun String.localizedDigits(): String = if (appLanguage() == LANGUAGE_BANGLA) toBengaliDigits() else this

private fun formatTimeTaken(startAt: String, endAt: String?): String {
    if (endAt == null) return "—"
    val start = parseServerTimeMillis(startAt) ?: return "—"
    val end = parseServerTimeMillis(endAt) ?: return "—"
    val totalSeconds = ((end - start) / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(Locale.ROOT, totalSeconds / 60, totalSeconds % 60)
}

const val RESULT_ANALYSIS_BUTTON_TAG = "result_analysis_button"
const val RESULT_RETAKE_BUTTON_TAG = "result_retake_button"
const val RESULT_ANSWER_SHEET_ROW_TAG = "result_answer_sheet_row"
const val RESULT_LEADERBOARD_ROW_TAG = "result_leaderboard_row"
