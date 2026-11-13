package com.prostuti.student.ui.screen.analysis

import com.prostuti.student.data.model.response.CurriculumName
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.domain.TopicAnalysisRow
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.FormFactorPreviews
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.ResultBar
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.ui.theme.extendedColors
import com.prostuti.student.util.LANGUAGE_BANGLA
import com.prostuti.student.util.toBengaliDigits
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

const val ANALYSIS_WEAKEST_TAG = "analysis_weakest"
const val ANALYSIS_TOPIC_TAG = "analysis_topic_"

/** Topics under this correct-answer share are flagged in the error color. */
private const val WEAK_PERCENT = 60

/**
 * Topic-wise result of one attempt. [onOpenTopic] gets the topic ID (null = the no-topic group)
 * and its display name, to open the answer sheet with just those questions.
 */
@Composable
fun AttemptAnalysisScreen(
    attemptId: String,
    titleBn: String,
    onBack: () -> Unit,
    onOpenTopic: (topicId: String?, topicName: String) -> Unit,
    /** False when shown beside the exam result (list-detail): the result stays on screen, so no back arrow. */
    showBack: Boolean = true,
    viewModel: AttemptAnalysisViewModel = koinViewModel(parameters = { parametersOf(attemptId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AttemptAnalysisContent(
        titleBn = titleBn,
        state = state,
        onBack = onBack,
        onOpenTopic = onOpenTopic,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
        showBack = showBack
    )
}

@Composable
fun AttemptAnalysisContent(
    titleBn: String,
    state: AttemptAnalysisUiState,
    onBack: () -> Unit,
    onOpenTopic: (topicId: String?, topicName: String) -> Unit,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit,
    showBack: Boolean = true
) {
    val locale = LocalConfiguration.current.locales[0]
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBack) {
                    BackButton(onClick = dropUnlessResumed { onBack() })
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = stringResource(R.string.analysis_title),
                        style = ProstutiTypography.bnTitleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (state.isLoading) titleBn
                        else stringResource(R.string.analysis_subtitle, titleBn, state.topics.size.toString().digits(locale)),
                        style = ProstutiTypography.bnLabelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            PullToRefreshErrorOverlayWrapper(
                errors = state.errors,
                errorCleaner = onClearError,
                pullToRefreshEnabled = true,
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh
            ) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProstutiSpinner()
                    }
                    state.topics.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.analysis_empty),
                            style = ProstutiTypography.bnBodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> AnalysisList(state = state, locale = locale, onOpenTopic = onOpenTopic)
                }
            }
        }
    }
}

@Composable
private fun AnalysisList(
    state: AttemptAnalysisUiState,
    locale: Locale,
    onOpenTopic: (String?, String) -> Unit
) {
    val noTopic = stringResource(R.string.analysis_no_topic)
    val unknownTopic = stringResource(R.string.analysis_unknown_topic)
    fun nameOf(row: TopicAnalysisRow) = when {
        row.topicId == null -> noTopic
        else -> row.topicName?.inLanguage(locale.language) ?: unknownTopic
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(adaptiveColumnCount()),
        state = rememberLazyGridState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.weakest?.let { weak ->
            item(key = "weakest", span = { GridItemSpan(maxLineSpan) }) {
                WeakestCard(
                    row = weak,
                    name = nameOf(weak),
                    locale = locale,
                    onOpen = dropUnlessResumed { onOpenTopic(weak.topicId, nameOf(weak)) }
                )
            }
        }
        item(key = "legend", span = { GridItemSpan(maxLineSpan) }) { ListHeader() }
        itemsIndexed(state.topics, key = { index, row -> row.topicId ?: "no_topic_$index" }) { _, row ->
            TopicRow(
                row = row,
                name = nameOf(row),
                chapter = if (row.topicId == null) stringResource(R.string.analysis_no_topic_detail) else row.chapterName?.inLanguage(locale.language),
                locale = locale,
                onClick = dropUnlessResumed { onOpenTopic(row.topicId, nameOf(row)) }
            )
        }
        item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.analysis_footer),
                style = ProstutiTypography.bnLabelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)
            )
        }
    }
}

@Composable
private fun WeakestCard(row: TopicAnalysisRow, name: String, locale: Locale, onOpen: () -> Unit) {
    val onContainer = MaterialTheme.colorScheme.onTertiaryContainer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .clickable(onClick = onOpen)
            .padding(14.dp)
            .testTag(ANALYSIS_WEAKEST_TAG),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = onContainer, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.analysis_weakest_label), style = ProstutiTypography.bnLabelMedium, color = onContainer)
        }
        Text(text = name, style = ProstutiTypography.bnTitleMedium, color = onContainer)
        Text(
            text = listOfNotNull(
                stringResource(
                    R.string.analysis_weakest_detail,
                    row.total.toString().digits(locale),
                    row.wrong.toString().digits(locale),
                    row.skipped.toString().digits(locale)
                ),
                row.chapterName?.inLanguage(locale.language)
            ).joinToString(" · "),
            style = ProstutiTypography.bnLabelMedium,
            color = onContainer
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Text(stringResource(R.string.analysis_weakest_open), style = ProstutiTypography.bnLabelLarge, color = onContainer)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = onContainer, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ListHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.analysis_list_title),
            style = ProstutiTypography.bnLabelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        LegendDot(MaterialTheme.extendedColors.success, stringResource(R.string.analysis_legend_correct))
        Spacer(Modifier.width(10.dp))
        LegendDot(MaterialTheme.colorScheme.error, stringResource(R.string.analysis_legend_wrong))
        Spacer(Modifier.width(10.dp))
        LegendDot(MaterialTheme.colorScheme.surfaceContainerHighest, stringResource(R.string.analysis_legend_skipped))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TopicRow(row: TopicAnalysisRow, name: String, chapter: String?, locale: Locale, onClick: () -> Unit) {
    val percent = row.correctPercent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(ANALYSIS_TOPIC_TAG + (row.topicId ?: "none")),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurface)
                if (!chapter.isNullOrBlank()) {
                    Text(chapter, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$percent%".digits(locale),
                    style = ProstutiTypography.enTitleMedium,
                    color = if (percent < WEAK_PERCENT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${row.correct}/${row.total}".digits(locale),
                    style = ProstutiTypography.enLabelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ResultBar(correct = row.correct, wrong = row.wrong, total = row.total, barHeight = 10.dp)
        Text(
            text = stringResource(
                R.string.history_counts,
                row.correct.toString().digits(locale),
                row.wrong.toString().digits(locale),
                row.skipped.toString().digits(locale)
            ),
            style = ProstutiTypography.bnLabelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun String.digits(locale: Locale): String = if (locale.language == LANGUAGE_BANGLA) toBengaliDigits() else this

@FormFactorPreviews
@Preview(name = "Phone dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AttemptAnalysisContentPreview() {
    ProstutiTheme {
        AttemptAnalysisContent(
            titleBn = "সাপ্তাহিক টেস্ট — পদার্থবিজ্ঞান ১ম পত্র",
            state = AttemptAnalysisUiState(
                isLoading = false,
                topics = listOf(
                    TopicAnalysisRow("t1", CurriculumName("সমত্বরণ গতি", ""), CurriculumName("গতিবিদ্যা", ""), 6, 3, 2, 1),
                    TopicAnalysisRow("t2", CurriculumName("প্রক্ষিপ্ত গতি", ""), CurriculumName("গতিবিদ্যা", ""), 4, 2, 2, 0),
                    TopicAnalysisRow("t3", CurriculumName("নিউটনের গতিসূত্র", ""), CurriculumName("নিউটনিয়ান বলবিদ্যা", ""), 5, 5, 0, 0),
                    TopicAnalysisRow(null, null, null, 3, 3, 0, 0)
                )
            ),
            onBack = {}, onOpenTopic = { _, _ -> }, onRefresh = {}, onClearError = {}
        )
    }
}
