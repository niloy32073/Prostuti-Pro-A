package com.prostuti.student.ui.screen.bookmark

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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.R
import com.prostuti.student.data.model.response.McqDetail
import com.prostuti.student.data.model.response.McqDetailOption
import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.service.local.BookmarkEntity
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.FormFactorPreviews
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.screen.bookmark.component.BookmarkedMcqCard
import com.prostuti.student.ui.theme.ProstutiTheme
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel

const val BOOKMARK_GRID_TAG = "bookmark_grid"
const val BOOKMARK_EMPTY_TAG = "bookmark_empty"

@Composable
fun BookmarkHistoryScreen(
    onBack: () -> Unit,
    viewModel: BookmarkHistoryViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BookmarkHistoryContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
        onToggleAnswer = viewModel::toggleAnswer,
        onRemove = viewModel::removeBookmark,
        onRetryMcq = viewModel::retryMcq
    )
}

/**
 * Bookmarked MCQs in a staggered grid: 1 column on compact, 2 on medium, 3 on expanded and up.
 * Each column stacks on its own, so opening one card's answer only pushes the cards below it
 * in that column — the other columns stay where they are, with no gaps left by uneven rows.
 */
@Composable
fun BookmarkHistoryContent(
    state: BookmarkHistoryUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit,
    onToggleAnswer: (mcqId: String) -> Unit,
    onRemove: (mcqId: String) -> Unit,
    onRetryMcq: (mcqId: String) -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.bookmark_history_title),
                        style = ProstutiTypography.bnTitleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.hasLoaded) {
                        Text(
                            text = stringResource(R.string.bookmark_history_count, state.bookmarks.size),
                            style = ProstutiTypography.bnLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            PullToRefreshErrorOverlayWrapper(
                errors = state.errors,
                errorCleaner = onClearError,
                errorTopPadding = 8.dp,
                pullToRefreshEnabled = true,
                isRefreshing = state.isLoading && state.hasLoaded,
                onRefresh = onRefresh
            ) {
                when {
                    state.bookmarks.isNotEmpty() -> BookmarkGrid(state, onToggleAnswer, onRemove, onRetryMcq)
                    !state.hasLoaded && state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProstutiSpinner()
                    }
                    else -> EmptyState(loadFailed = !state.hasLoaded)
                }
            }
        }
    }
}

@Composable
private fun BookmarkGrid(
    state: BookmarkHistoryUiState,
    onToggleAnswer: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRetryMcq: (String) -> Unit
) {
    val columns = adaptiveColumnCount()
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = rememberLazyStaggeredGridState(),
        modifier = Modifier.fillMaxSize().testTag(BOOKMARK_GRID_TAG),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalItemSpacing = 14.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(state.bookmarks, key = { _, bookmark -> bookmark.resourceId }) { index, bookmark ->
            BookmarkedMcqCard(
                index = index,
                bookmark = bookmark,
                mcq = state.mcqs[bookmark.resourceId],
                failed = bookmark.resourceId in state.failedMcqIds,
                isAnswerRevealed = bookmark.resourceId in state.revealedMcqIds,
                onToggleAnswer = { onToggleAnswer(bookmark.resourceId) },
                onRemove = { onRemove(bookmark.resourceId) },
                onRetry = { onRetryMcq(bookmark.resourceId) },
                // Slides the rest of the column when a card is removed.
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun EmptyState(loadFailed: Boolean) {
    // Scrollable so pull-to-refresh works on it too.
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag(BOOKMARK_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.padding(top = 80.dp))
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = stringResource(if (loadFailed) R.string.bookmark_history_load_failed else R.string.bookmark_history_empty),
            style = ProstutiTypography.bnBodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        if (!loadFailed) {
            Text(
                text = stringResource(R.string.bookmark_history_empty_hint),
                style = ProstutiTypography.bnBodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun previewMcq(id: String, text: String, explanation: String?) = McqDetail(
    id = id,
    questionText = text,
    options = listOf("মিটার", "কিলোগ্রাম", "সেকেন্ড", "অ্যাম্পিয়ার").mapIndexed { i, o ->
        McqDetailOption(id = "$id-$i", optionText = o, order = i, isCorrect = i == 0)
    },
    explanation = explanation
)

private val previewState = BookmarkHistoryUiState(
    hasLoaded = true,
    bookmarks = (1..5).map { BookmarkEntity(BookmarkResourceType.MCQ, "m$it", "b$it", createdAt = "2026-10-2${it}T10:00:00Z") },
    mcqs = mapOf(
        "m1" to previewMcq("m1", "দৈর্ঘ্যের SI একক কী?", "SI পদ্ধতিতে দৈর্ঘ্যের একক মিটার।"),
        "m2" to previewMcq("m2", "নিচের কোনটি মৌলিক রাশি?", null),
        "m4" to previewMcq("m4", "তড়িৎ প্রবাহের একক কোনটি?", "তড়িৎ প্রবাহের SI একক অ্যাম্পিয়ার।")
    ),
    failedMcqIds = setOf("m5"),
    revealedMcqIds = setOf("m1")
)

@FormFactorPreviews
@Composable
private fun BookmarkHistoryContentPreview() {
    ProstutiTheme {
        BookmarkHistoryContent(
            state = previewState,
            onBack = {}, onRefresh = {}, onClearError = {}, onToggleAnswer = {}, onRemove = {}, onRetryMcq = {}
        )
    }
}
