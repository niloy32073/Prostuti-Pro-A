package com.prostuti.student.ui.screen.group

import androidx.compose.foundation.background
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
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.theme.ProstutiTypography

/** The "খুঁজে দেখো" tab of GroupList. Errors show through GroupList's overlay. */
@Composable
fun GroupExploreTab(
    state: GroupExploreUiState,
    onQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onRequestToJoin: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query, onValueChange = onQueryChange, singleLine = true,
            placeholder = { Text(stringResource(R.string.group_explore_search), style = ProstutiTypography.bnBodyMedium) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag(TAG_EXPLORE_SEARCH)
        )
        if (state.isLoading && state.groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ProstutiSpinner()
            }
            return@Column
        }
        val gridState = rememberLazyGridState()
        val nearEnd by remember {
            derivedStateOf {
                val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= gridState.layoutInfo.totalItemsCount - 3
            }
        }
        LaunchedEffect(nearEnd, state.canLoadMore) { if (nearEnd && state.canLoadMore) onLoadMore() }

        val columns = adaptiveColumnCount()
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 10.dp else 0.dp)
        ) {
            if (state.groups.isEmpty() && !state.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.group_explore_empty), style = ProstutiTypography.bnBodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
            items(state.groups, key = { it.id }) { group ->
                ExploreCard(
                    group = group,
                    isRequesting = group.id in state.requesting,
                    onOpen = dropUnlessResumed { onOpenGroup(group.id) },
                    onRequest = { onRequestToJoin(group.id) },
                )
            }
            if (state.isLoadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        ProstutiSpinner(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreCard(group: StudyGroup, isRequesting: Boolean, onOpen: () -> Unit, onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupAvatar(name = group.name)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.group_explore_members, localizedNumber(group.memberCount), localizedNumber(group.maxMembers)),
                    style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        group.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = ProstutiTypography.bnBodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        val tag = "$TAG_EXPLORE_ACTION_PREFIX${group.id}"
        when (group.exploreAction()) {
            ExploreAction.OPEN -> PrimaryButton(text = stringResource(R.string.group_explore_open), onClick = onOpen, modifier = Modifier.testTag(tag))
            ExploreAction.REQUEST -> PrimaryButton(
                text = stringResource(R.string.group_explore_request), onClick = onRequest, loading = isRequesting, enabled = !isRequesting,
                modifier = Modifier.testTag(tag)
            )
            ExploreAction.REQUESTED -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().testTag(tag)) {
                Text(stringResource(R.string.group_explore_requested), style = ProstutiTypography.bnLabelLarge)
            }
            ExploreAction.FULL -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().testTag(tag)) {
                Text(stringResource(R.string.group_explore_full), style = ProstutiTypography.bnLabelLarge)
            }
        }
    }
}

const val TAG_EXPLORE_SEARCH = "group_explore_search"
const val TAG_EXPLORE_ACTION_PREFIX = "group_explore_action_"
