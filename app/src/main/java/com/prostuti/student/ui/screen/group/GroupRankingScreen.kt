package com.prostuti.student.ui.screen.group

import com.prostuti.student.ui.component.paneIsMediumOrWider
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.data.model.response.LeaderboardEntry
import com.prostuti.student.data.repository.GroupExamStats
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.screen.routine.decimalText
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

const val TAG_RANK_SUMMARY = "rank_summary"
const val TAG_RANK_ROW_PREFIX = "rank_row_"
const val TAG_RANK_NOT_READY = "rank_not_ready"
const val TAG_RANK_MY_RESULT = "rank_my_result"

@Composable
fun GroupRankingScreen(
    groupId: String,
    examId: String,
    titleBn: String,
    closesAt: String?,
    onBack: () -> Unit,
    onOpenMyAnswerSheet: (attemptId: String) -> Unit,
    onOpenMyResult: (attemptId: String) -> Unit,
    viewModel: GroupRankingViewModel = koinViewModel(parameters = { parametersOf(groupId, examId, closesAt) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupRankingContent(
        state = state, titleBn = titleBn, onBack = onBack, onRefresh = viewModel::refresh, onLoadMore = viewModel::loadMore,
        onOpenMyAnswerSheet = onOpenMyAnswerSheet, onOpenMyResult = onOpenMyResult, onClearError = viewModel::clearError,
    )
}

/** The design's GroupRanking: three tiles, the score spread, then the rank list with my row highlighted. */
@Composable
fun GroupRankingContent(
    state: GroupRankingUiState,
    titleBn: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onOpenMyAnswerSheet: (String) -> Unit = {},
    onOpenMyResult: (String) -> Unit = {},
    onClearError: (String) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= listState.layoutInfo.totalItemsCount - 3 } }
    LaunchedEffect(nearEnd, state.entries.size) { if (nearEnd) onLoadMore() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors, errorCleaner = onClearError, overlayPadding = padding,
            pullToRefreshEnabled = true, isRefreshing = state.isLoading && state.page > 0, onRefresh = onRefresh,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.ranking_title), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            listOfNotNull(state.groupName, titleBn).joinToString(" · "), style = ProstutiTypography.bnLabelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                if (state.isLoading && state.page == 0 && !state.notReady) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ProstutiSpinner() }
                    return@Column
                }
                val notReady: @Composable () -> Unit = {
                    Text(
                        stringResource(if (state.windowOpen) R.string.ranking_window_open else R.string.ranking_not_ready),
                        style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
                            .padding(16.dp).testTag(TAG_RANK_NOT_READY)
                    )
                }
                val myResult: @Composable (String) -> Unit = { id ->
                    OutlinedButton(onClick = dropUnlessResumed { onOpenMyResult(id) }, modifier = Modifier.fillMaxWidth().testTag(TAG_RANK_MY_RESULT)) {
                        Text(stringResource(R.string.ranking_my_result), style = ProstutiTypography.bnLabelLarge)
                    }
                }
                val rows: LazyListScope.() -> Unit = {
                    items(state.entries, key = { it.userId }) { entry ->
                        val isMe = entry.userId == state.myUserId
                        val myAttempt = state.myAttemptId
                        RankRow(entry, isMe, onClick = if (isMe && myAttempt != null) dropUnlessResumed { onOpenMyAnswerSheet(myAttempt) } else null)
                    }
                    if (state.isLoadingMore) {
                        item { Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { ProstutiSpinner(Modifier.size(24.dp)) } }
                    }
                }
                if (paneIsMediumOrWider()) {
                    // Medium and up: the summary stays in view on the left while the rank list scrolls.
                    Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(
                            Modifier.weight(0.4f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 16.dp).testTag(TAG_RANK_SUMMARY),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatsRow(state)
                            state.stats?.let { Histogram(it) }
                            state.myAttemptId?.let { myResult(it) }
                        }
                        LazyColumn(
                            state = listState, modifier = Modifier.weight(0.6f).fillMaxHeight(),
                            contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.notReady || state.windowOpen) item { notReady() }
                            rows()
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { StatsRow(state) }
                        state.stats?.let { stats -> item { Histogram(stats) } }
                        state.myAttemptId?.let { id -> item { myResult(id) } }
                        if (state.notReady || state.windowOpen) item { notReady() }
                        rows()
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(state: GroupRankingUiState) {
    val members = state.memberCount
    val took = if (members != null) stringResource(R.string.ranking_took_part_value, localizedNumber(state.totalParticipants), localizedNumber(members))
    else localizedNumber(state.totalParticipants)
    val myRank = when {
        state.myEntry != null -> "#" + localizedNumber(state.myEntry!!.rank)
        else -> "—"
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(took, stringResource(R.string.ranking_took_part), Modifier.weight(1f))
        // DUMMY until the API gives an average (GROUP_ROUTINE_PLAN.md §14 B1).
        StatTile(state.stats?.let { localizedNumber(it.averagePercent) + "%" } ?: "—", stringResource(R.string.ranking_average), Modifier.weight(1f))
        StatTile(myRank, stringResource(if (state.myAttemptId == null && !state.isLoading) R.string.ranking_not_ranked else R.string.ranking_my_rank), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}

/** DUMMY spread (§14 B1): ten bars, 0 % to 100 %. */
@Composable
private fun Histogram(stats: GroupExamStats) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.ranking_spread), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth().height(70.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
            stats.histogram.forEach { value ->
                Box(Modifier.weight(1f).height((value.coerceIn(4, 100) * 0.7f).dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primaryContainer))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(localizedNumber(0) + "%", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(localizedNumber(100) + "%", style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RankRow(entry: LeaderboardEntry, isMe: Boolean, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(12.dp).testTag(TAG_RANK_ROW_PREFIX + entry.userId),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val top = entry.rank <= 3
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(if (top) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Text("#" + localizedNumber(entry.rank), style = ProstutiTypography.bnLabelMedium,
                color = if (top) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (isMe) stringResource(R.string.ranking_me, entry.username) else entry.username,
            style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(decimalText(entry.score), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(minSecText(entry.timeTakenSec), style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun minSecText(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val sec = localizedNumber(s % 60)
    return localizedNumber(s / 60) + ":" + (if (s % 60 < 10) localizedNumber(0) else "") + sec
}
