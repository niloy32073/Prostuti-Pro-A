package com.prostuti.student.ui.screen.group

import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.material.icons.filled.Add
import com.prostuti.student.ui.component.ProstutiSpinner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.prostuti.student.R
import com.prostuti.student.ui.screen.routine.minuteOfDayText
import com.prostuti.student.ui.screen.routine.slot.RefreshAtWindowEdges
import com.prostuti.student.ui.screen.routine.slot.TodayBadge
import com.prostuti.student.ui.screen.routine.slot.rememberNow
import com.prostuti.student.ui.screen.routine.slot.remainingText
import com.prostuti.student.util.DhakaTime
import java.time.Instant
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.ui.component.BackButton
import com.prostuti.student.ui.component.PrimaryButton
import com.prostuti.student.ui.component.PullToRefreshErrorOverlayWrapper
import com.prostuti.student.ui.component.adaptiveColumnCount
import com.prostuti.student.ui.component.localizedNumber
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    onBack: () -> Unit,
    onNavigateToGroup: (String) -> Unit,
    /** The group open beside the list (two panes), highlighted. */
    selectedGroupId: String? = null,
    /** The + button: the GroupCreateJoin sheet (it shares this screen's ViewModel). */
    onOpenCreateJoin: () -> Unit = {},
    viewModel: GroupListViewModel = koinViewModel(),
    exploreViewModel: GroupExploreViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exploreState by exploreViewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_MINE) }

    // The retained ViewModel doesn't reload by itself after returning from a GroupDetail visit
    // (where I may have left the group, or it was deleted).
    LaunchedEffect(Unit) { viewModel.refresh() }
    RefreshAtWindowEdges(slots = state.todaySlots.values.flatten(), onRefresh = viewModel::refresh)

    LaunchedEffect(state.navigateToGroupId) {
        state.navigateToGroupId?.let {
            viewModel.consumeNavigation()
            onNavigateToGroup(it)
        }
    }

    GroupListContent(
        state = state,
        onBack = onBack,
        onAdd = onOpenCreateJoin,
        onOpenGroup = onNavigateToGroup,
        selectedGroupId = selectedGroupId,
        onRefresh = if (selectedTab == TAB_MINE) viewModel::refresh else exploreViewModel::refresh,
        onClearError = viewModel::clearError,
        selectedTab = selectedTab,
        onSelectTab = { selectedTab = it },
        exploreErrors = exploreState.errors,
        onClearExploreError = exploreViewModel::clearError,
        isExploreRefreshing = exploreState.isLoading && exploreState.groups.isNotEmpty(),
    ) {
        GroupExploreTab(
            state = exploreState,
            onQueryChange = exploreViewModel::onQueryChange,
            onLoadMore = exploreViewModel::loadMore,
            onOpenGroup = onNavigateToGroup,
            onRequestToJoin = exploreViewModel::requestToJoin,
        )
    }
}

const val TAB_MINE = 0
const val TAB_EXPLORE = 1
private const val EXPLORE_ERROR_PREFIX = "explore:"


@Composable
fun GroupListContent(
    state: GroupListUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onRefresh: () -> Unit,
    onClearError: (String) -> Unit,
    selectedGroupId: String? = null,
    selectedTab: Int = TAB_MINE,
    onSelectTab: (Int) -> Unit = {},
    exploreErrors: Map<String, String> = emptyMap(),
    onClearExploreError: (String) -> Unit = {},
    isExploreRefreshing: Boolean = false,
    /** Fixed time for tests; null = the ticking clock. */
    clock: Instant? = null,
    exploreContent: @Composable () -> Unit = {},
) {
    val ticking by rememberNow()
    val now = clock ?: ticking
    val onMine = selectedTab == TAB_MINE
    // One overlay for both tabs; explore's tags are prefixed so each clear goes to its own ViewModel.
    val errors = if (onMine) state.errors else exploreErrors.mapKeys { EXPLORE_ERROR_PREFIX + it.key }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Create or join a group: the same floating button as GroupMembers' "add a member".
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.group_create_tab), style = ProstutiTypography.bnLabelLarge) },
            )
        }
    ) { padding ->
        PullToRefreshErrorOverlayWrapper(
            errors = errors,
            errorCleaner = { tag -> if (tag.startsWith(EXPLORE_ERROR_PREFIX)) onClearExploreError(tag.removePrefix(EXPLORE_ERROR_PREFIX)) else onClearError(tag) },
            overlayPadding = padding,
            pullToRefreshEnabled = true,
            isRefreshing = if (onMine) state.isLoading && state.groups.isNotEmpty() else isExploreRefreshing,
            onRefresh = onRefresh
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.group_list_title), style = ProstutiTypography.bnTitleLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = stringResource(R.string.group_list_count, localizedNumber(state.groups.size)),
                            style = ProstutiTypography.enBodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = onMine, onClick = { onSelectTab(TAB_MINE) },
                        text = { Text(stringResource(R.string.group_tab_mine), style = ProstutiTypography.bnLabelLarge) }
                    )
                    Tab(
                        selected = !onMine, onClick = { onSelectTab(TAB_EXPLORE) },
                        modifier = Modifier.testTag(TAG_TAB_EXPLORE),
                        text = { Text(stringResource(R.string.group_tab_explore), style = ProstutiTypography.bnLabelLarge) }
                    )
                }

                when {
                    !onMine -> exploreContent()
                    state.isLoading && state.groups.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ProstutiSpinner()
                    }
                    else -> {
                        val columns = adaptiveColumnCount()
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = rememberLazyGridState(),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (columns > 1) 10.dp else 0.dp)
                        ) {
                            if (state.groups.isEmpty() && state.errors.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) { EmptyGroups(onAdd = onAdd) }
                            }
                            items(state.groups, key = { it.id }) { group ->
                                GroupListCard(
                                    group = group, badge = TodayBadge.of(state.todaySlots[group.id].orEmpty(), now), now = now,
                                    selected = group.id == selectedGroupId,
                                    onClick = dropUnlessResumed { onOpenGroup(group.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGroups(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.group_list_empty), style = ProstutiTypography.bnTitleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            stringResource(R.string.group_list_empty_hint), style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        PrimaryButton(text = stringResource(R.string.group_list_add_cd), onClick = onAdd, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun GroupListCard(group: StudyGroup, badge: TodayBadge, now: Instant, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupAvatar(name = group.name)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name, style = ProstutiTypography.bnBodyLarge, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            val role = stringResource(if (group.myRole == GroupRole.ADMIN) R.string.group_role_admin_you else R.string.group_role_member)
            Text(
                text = stringResource(R.string.group_member_count, localizedNumber(group.memberCount)) + " · " + role,
                style = ProstutiTypography.enLabelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp)
            )
            TodayBadgeText(badge, now, group.id)
        }
    }
}

/** §6.4: red "window open" banner, "opens today at …", a green "done", or "no exam running". */
@Composable
private fun TodayBadgeText(badge: TodayBadge, now: Instant, groupId: String) {
    val (text, color) = when (badge) {
        is TodayBadge.Open -> {
            val left = remainingText(now, DhakaTime.parseInstant(badge.slot.closesAt) ?: now)
            val title = badge.slot.title
            (if (title != null) stringResource(R.string.group_badge_open, title, left) else stringResource(R.string.group_badge_open_untitled, left)) to MaterialTheme.colorScheme.error
        }
        is TodayBadge.Upcoming -> {
            val opens = DhakaTime.parseInstant(badge.slot.opensAt)?.toEpochMilli() ?: 0L
            stringResource(R.string.group_badge_upcoming, minuteOfDayText(DhakaTime.minuteOfDayAt(opens))) to MaterialTheme.colorScheme.primary
        }
        TodayBadge.Done -> stringResource(R.string.group_badge_done) to MaterialTheme.colorScheme.tertiary
        TodayBadge.None -> stringResource(R.string.group_badge_none) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, style = ProstutiTypography.bnLabelMedium, color = color, modifier = Modifier.padding(top = 4.dp).testTag(TAG_GROUP_BADGE_PREFIX + groupId))
}

const val TAG_GROUP_BADGE_PREFIX = "group_badge_"

const val TAG_TAB_EXPLORE = "group_tab_explore"

/** A group's square avatar: the first two letters of its name. */
@Composable
internal fun GroupAvatar(name: String) {
    Box(
        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name.trim().take(2), style = ProstutiTypography.bnLabelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
