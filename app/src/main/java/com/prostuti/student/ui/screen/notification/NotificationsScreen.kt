package com.prostuti.student.ui.screen.notification

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.R
import com.prostuti.student.data.model.response.NotificationItem
import com.prostuti.student.domain.notification.NotificationTarget
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.screen.group.component.groupDateTimeText
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel

const val TAG_NOTIF_ROW_PREFIX = "notif_row_"
const val TAG_NOTIF_UNREAD_DOT = "notif_unread_dot"
const val TAG_NOTIF_FILTER_UNREAD = "notif_filter_unread"
const val TAG_NOTIF_EMPTY = "notif_empty"

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenTarget: (NotificationTarget) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.target) {
        state.target?.let {
            viewModel.consumeTarget()
            onOpenTarget(it)
        }
    }
    NotificationsContent(
        state = state, onBack = onBack, onUnreadOnly = viewModel::setUnreadOnly, onOpen = viewModel::open,
        onLoadMore = viewModel::loadMore, onRefresh = viewModel::refresh, onClearError = viewModel::clearError,
    )
}

@Composable
fun NotificationsContent(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onUnreadOnly: (Boolean) -> Unit = {},
    onOpen: (NotificationItem) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onClearError: (String) -> Unit = {},
) {
    val gridState = rememberLazyGridState()
    val nearEnd by remember { derivedStateOf { (gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= gridState.layoutInfo.totalItemsCount - 4 } }
    LaunchedEffect(nearEnd, state.items.size) { if (nearEnd) onLoadMore() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors, errorCleaner = onClearError, overlayPadding = padding,
            pullToRefreshEnabled = true, isRefreshing = state.isLoading && state.items.isNotEmpty(), onRefresh = onRefresh,
        ) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.notif_title), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onBackground)
                }
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !state.unreadOnly, onClick = { onUnreadOnly(false) }, label = { Text(stringResource(R.string.notif_all)) })
                    FilterChip(
                        selected = state.unreadOnly, onClick = { onUnreadOnly(true) }, label = { Text(stringResource(R.string.notif_unread)) },
                        modifier = Modifier.testTag(TAG_NOTIF_FILTER_UNREAD)
                    )
                }
                if (state.isLoading && state.items.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ProstutiSpinner() }
                    return@Column
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(adaptiveColumnCount()), state = gridState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.items.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                stringResource(if (state.unreadOnly) R.string.notif_empty_unread else R.string.notif_empty),
                                style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 32.dp).testTag(TAG_NOTIF_EMPTY)
                            )
                        }
                    }
                    items(state.items, key = { it.id }) { NotificationRow(it, onClick = { onOpen(it) }) }
                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ProstutiSpinner(Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

/** Unread: bold title and a dot on the left. Title and message are shown as the server wrote them. */
@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
    val unreadCd = stringResource(R.string.notif_unread)
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(14.dp).testTag(TAG_NOTIF_ROW_PREFIX + item.id),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.padding(top = 6.dp).size(8.dp)) {
            if (!item.isRead) {
                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape).testTag(TAG_NOTIF_UNREAD_DOT)
                    .semantics { contentDescription = unreadCd })
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title, style = ProstutiTypography.bnBodyLarge.copy(fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(item.message, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(groupDateTimeText(item.createdAt), style = ProstutiTypography.bnLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
