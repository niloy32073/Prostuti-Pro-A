package com.prostuti.student.ui.screen.qb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.R
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.screen.examengine.component.McqCard
import com.prostuti.student.ui.screen.examengine.component.McqGridItem
import com.prostuti.student.ui.screen.examengine.component.PassageCard
import com.prostuti.student.ui.theme.ProstutiTypography

@Composable
fun QuestionReaderScreen(
    titleBn: String,
    onBack: () -> Unit,
    viewModel: QuestionReaderViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    QuestionReaderContent(
        titleBn = titleBn,
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
        onToggleFavorite = viewModel::toggleFavorite,
        onToggleAnswer = viewModel::toggleAnswer,
        onToggleAllAnswers = viewModel::toggleAllAnswers,
        onReport = viewModel::submitReport,
        onReportSentConsumed = viewModel::onReportSentConsumed
    )
}

/** Stateless reader: questions + options, answer reveal and an explanation popup. */
@Composable
fun QuestionReaderContent(
    titleBn: String,
    state: QuestionReaderUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleAnswer: (String) -> Unit,
    onToggleAllAnswers: () -> Unit,
    onReport: (questionId: String, reason: String, detail: String) -> Unit,
    onReportSentConsumed: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val reportSentMessage = stringResource(R.string.mcq_report_sent)
    LaunchedEffect(state.isReportSent) {
        if (state.isReportSent) {
            onReportSentConsumed()
            snackbarHostState.showSnackbar(reportSentMessage)
        }
    }

    // mcqId whose explanation popup is open; survives rotation.
    var explanationFor by rememberSaveable { mutableStateOf<String?>(null) }
    if (explanationFor != null) {
        ExplanationDialog(onDismiss = { explanationFor = null })
    }

    val cardActions = ReaderCardActions(
        onToggleFavorite = onToggleFavorite,
        onToggleAnswer = onToggleAnswer,
        onShowExplanation = { explanationFor = it },
        onReport = onReport
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleBn,
                        style = ProstutiTypography.bnTitleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (state.questionCount > 0) {
                        Text(
                            text = stringResource(R.string.qb_reader_question_count, state.questionCount),
                            style = ProstutiTypography.enLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (state.questionCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.qb_reader_all_answers),
                            style = ProstutiTypography.bnLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = state.showAllAnswers,
                            onCheckedChange = { onToggleAllAnswers() },
                            modifier = Modifier.testTag(READER_ALL_ANSWERS_SWITCH_TAG),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            PullToRefreshErrorOverlayWrapper(
                errors = state.errors,
                errorCleaner = onClearError,
                modifier = Modifier.weight(1f),
                pullToRefreshEnabled = true,
                isRefreshing = state.isLoading,
                onRefresh = onRefresh
            ) {
                val columns = adaptiveColumnCount()
                val hPadding = if (columns > 1) 8.dp else 16.dp
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    state = rememberLazyStaggeredGridState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = hPadding, end = hPadding, bottom = 24.dp),
                    verticalItemSpacing = 14.dp,
                    horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 8.dp else 0.dp)
                ) {
                    if (!state.isLoading && state.items.isEmpty() && state.errors.isEmpty()) {
                        item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = stringResource(R.string.qb_reader_no_questions),
                                style = ProstutiTypography.bnBodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp)
                            )
                        }
                    }
                    items(state.items, key = { it.key }) { item ->
                        when (item) {
                            is McqGridItem.Standalone -> ReaderMcqCard(
                                number = item.index,
                                item = item.question,
                                state = state,
                                actions = cardActions
                            )

                            is McqGridItem.PassageGroup -> PassageCard(passage = item.passage) {
                                item.items.forEachIndexed { groupIndex, (number, question) ->
                                    if (groupIndex > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                    ReaderMcqCard(
                                        number = number,
                                        item = question,
                                        state = state,
                                        actions = cardActions,
                                        showContainer = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderMcqCard(
    number: Int,
    item: Question,
    state: QuestionReaderUiState,
    actions: ReaderCardActions,
    showContainer: Boolean = true
) {
    McqCard(
        index = number,
        question = item,
        selectedOptionId = null,
        onOptionSelected = {},
        isFavorite = item.mcqId in state.favoriteIds,
        onToggleFavorite = { actions.onToggleFavorite(item.mcqId) },
        onReport = { reason, detail -> actions.onReport(item.mcqId, reason, detail) },
        showContainer = showContainer,
        readOnly = true,
        isAnswerRevealed = state.showAllAnswers || item.mcqId in state.revealedAnswers,
        onToggleAnswer = { actions.onToggleAnswer(item.mcqId) },
        onShowExplanation = { actions.onShowExplanation(item.mcqId) },
        isReported = item.mcqId in state.reportedIds
    )
}

/** Per-card callbacks, bundled so passage and standalone cards get the same set. */
private class ReaderCardActions(
    val onToggleFavorite: (String) -> Unit,
    val onToggleAnswer: (String) -> Unit,
    val onShowExplanation: (String) -> Unit,
    val onReport: (questionId: String, reason: String, detail: String) -> Unit
)

/** Placeholder until the explanation API exists. */
@Composable
private fun ExplanationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.bookmark_explanation), style = ProstutiTypography.bnTitleMedium) },
        text = {
            Text(
                text = stringResource(R.string.qb_reader_explanation_coming),
                style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.qb_reader_close), style = ProstutiTypography.bnBodyMedium)
            }
        }
    )
}

const val READER_ALL_ANSWERS_SWITCH_TAG = "reader_all_answers_switch"
