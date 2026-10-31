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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.screen.group.component.GroupCreateJoinSheet
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.util.toBengaliDigits
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    onBack: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    viewModel: GroupListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    // Catches up on badges (e.g. a group's exam got submitted or closed) after returning from
    // a GroupDetail visit — the retained ViewModel doesn't otherwise reload on its own.
    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(state.navigateToGroupId) {
        state.navigateToGroupId?.let {
            onNavigateToGroup(it)
            viewModel.consumeNavigation()
        }
    }

    if (state.showCreateJoinSheet) {
        GroupCreateJoinSheet(
            sheetState = sheetState,
            isSubmitting = state.isSubmittingCreateJoin,
            errorBn = state.createJoinErrorBn,
            onCreate = viewModel::createGroup,
            onJoin = viewModel::joinGroup,
            onDismiss = viewModel::dismissCreateJoinSheet
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = state.errors,
            errorCleaner = viewModel::clearError,
            overlayPadding = padding,
            pullToRefreshEnabled = true,
            isRefreshing = state.isLoading && state.rows.isNotEmpty(),
            onRefresh = viewModel::refresh
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "আমার গ্রুপ", style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "${state.rows.size.toBengaliDigits()}টি গ্রুপে আছো",
                            style = ProstutiTypography.enBodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = viewModel::openCreateJoinSheet),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = viewModel::openCreateJoinSheet) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "নতুন গ্রুপ তৈরি/জয়েন করো",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (state.isLoading && state.rows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    val columns = adaptiveColumnCount()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = rememberLazyGridState(),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 10.dp else 0.dp)
                    ) {
                        items(state.rows, key = { it.group.id }) { row ->
                            GroupListCard(row = row, onClick = { onNavigateToGroup(row.group.id) })
                        }
                    }
                }
            }
    
        }
    }
}

@Composable
private fun GroupListCard(row: GroupListRow, onClick: () -> Unit) {
    val group = row.group
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = group.nameBn.take(2),
                style = ProstutiTypography.bnLabelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.nameBn, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            val roleLabel = if (group.myRole == GroupRole.ADMIN) "তুমি অ্যাডমিন" else "সাধারণ সদস্য"
            Text(
                text = "${group.memberCount.toBengaliDigits()} সদস্য · $roleLabel",
                style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        GroupListStatusBadge(badge = row.badge, labelBn = row.badgeLabelBn)
    }
}

@Composable
private fun GroupListStatusBadge(badge: GroupListBadge, labelBn: String) {
    val (bg, fg) = when (badge) {
        GroupListBadge.OPEN -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        GroupListBadge.SUBMITTED, GroupListBadge.CLOSED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        GroupListBadge.NO_ROUTINE, GroupListBadge.NONE -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 5.dp)) {
        Text(text = labelBn, style = ProstutiTypography.bnLabelMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
