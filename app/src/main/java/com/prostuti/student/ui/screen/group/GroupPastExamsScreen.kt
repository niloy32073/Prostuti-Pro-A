package com.prostuti.student.ui.screen.group

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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.SlotMyStatus
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.repository.GroupExamStats
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.screen.routine.routineDateText
import com.prostuti.student.ui.screen.routine.slot.date
import com.prostuti.student.ui.screen.routine.topicLabelText
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

const val TAG_PAST_ROW_PREFIX = "past_row_"
const val TAG_PAST_EMPTY = "past_empty"

@Composable
fun GroupPastExamsScreen(
    groupId: String,
    onBack: () -> Unit,
    onOpenRanking: (slot: RoutineSlot, title: String) -> Unit,
    viewModel: GroupPastExamsViewModel = koinViewModel(parameters = { parametersOf(groupId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    GroupPastExamsContent(state, onBack, onOpenRanking, viewModel::refresh, viewModel::clearError)
}

@Composable
fun GroupPastExamsContent(
    state: GroupPastExamsUiState,
    onBack: () -> Unit,
    onOpenRanking: (RoutineSlot, String) -> Unit,
    onRefresh: () -> Unit = {},
    onClearError: (String) -> Unit = {},
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors, errorCleaner = onClearError, overlayPadding = padding,
            pullToRefreshEnabled = true, isRefreshing = state.isLoading && state.slots.isNotEmpty(), onRefresh = onRefresh,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.past_title), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                        state.groupName?.let { Text(it, style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                if (state.isLoading && state.slots.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ProstutiSpinner() }
                    return@Column
                }
                val columns = adaptiveColumnCount()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns), state = rememberLazyGridState(), modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.slots.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                stringResource(R.string.past_empty), style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 32.dp).testTag(TAG_PAST_EMPTY)
                            )
                        }
                    }
                    items(state.slots, key = { it.id }) { slot ->
                        val title = slot.title ?: topicLabelText(state.labels[slot.id] ?: TopicLabel.Count(slot.topicIds.size))
                        PastExamRow(slot, title, state.stats[slot.id], onClick = dropUnlessResumed { onOpenRanking(slot, title) })
                    }
                }
            }
        }
    }
}

/** Trophy, name, "{date} · taken/missed", and the DUMMY "{n} submitted · average {x}%" (§14 B1). */
@Composable
internal fun PastExamRow(slot: RoutineSlot, title: String, stats: GroupExamStats?, onClick: () -> Unit) {
    // Without an exam (nobody ever started it) there is no ranking to open.
    val canOpen = slot.examId != null
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .let { if (canOpen) it.clickable(onClick = onClick) else it }.padding(12.dp).testTag(TAG_PAST_ROW_PREFIX + slot.id),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val mine = stringResource(if (slot.myStatus == SlotMyStatus.COMPLETED || slot.myStatus == SlotMyStatus.IN_PROGRESS) R.string.past_took else R.string.past_missed)
            Text(
                stringResource(R.string.past_row_meta, slot.date?.let { routineDateText(it) } ?: slot.slotDate, mine),
                style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            stats?.let {
                Text(
                    stringResource(R.string.past_row_stats, localizedNumber(it.submittedCount), localizedNumber(it.averagePercent)),
                    style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
