package com.prostuti.student.ui.screen.home

import com.prostuti.student.domain.FeatureAccess
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.ui.component.UserAvatar
import com.prostuti.student.ui.screen.home.component.AdmissionReminderCard
import com.prostuti.student.data.model.response.UnfinishedAttemptSummary
import com.prostuti.student.ui.screen.home.component.ContinueAttemptCard
import com.prostuti.student.ui.screen.home.component.ExamPreviewCardView
import com.prostuti.student.ui.screen.home.component.GroupExamPreviewCard
import com.prostuti.student.ui.screen.routine.slot.RefreshAtWindowEdges
import com.prostuti.student.ui.screen.routine.slot.rememberNow
import java.time.Instant
import com.prostuti.student.ui.screen.home.component.LeaderboardSection
import com.prostuti.student.ui.screen.home.component.QuickMenuItem
import com.prostuti.student.ui.screen.home.component.QuickMenuSection
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography
import com.prostuti.student.ui.screen.home.component.WidgetPromoCard
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onNavigateToQB: () -> Unit = {},
    onNavigateToPractice: () -> Unit = {},
    onNavigateToGroupList: () -> Unit = {},
    onNavigateToLiveExamList: () -> Unit = {},
    onNavigateToGroupDetail: (groupId: String) -> Unit = {},
    onContinueAttempt: (attemptId: String, titleBn: String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit = {},
    onNavigateToPlayground: () -> Unit = {},
    onNavigateToRoutine: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
    groupExamViewModel: HomeGroupExamViewModel = koinViewModel(),
    notificationsViewModel: HomeNotificationsViewModel = koinViewModel(),
    widgetPromoViewModel: HomeWidgetPromoViewModel = koinViewModel()
) {
    val widgetPromo by widgetPromoViewModel.state.collectAsStateWithLifecycle()
    val hasUnreadNotifications by notificationsViewModel.hasUnread.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val groupExamState by groupExamViewModel.state.collectAsStateWithLifecycle()
    val now by rememberNow()
    // No `group:use` scope: no group menu item and no group exam card (§6.20).
    val access = FeatureAccess.of(state.profile)
    val hiddenMenu = if (access.group) emptySet() else setOf(R.string.qm_group)
    val groupExam = groupExamState.exam.takeIf { access.group }

    // Re-fetch on every return to Home so a just-finished or just-started exam shows up at once.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshContinueAttempt()
        groupExamViewModel.refresh()
        notificationsViewModel.refresh()
        widgetPromoViewModel.refresh()
        onPauseOrDispose { }
    }
    RefreshAtWindowEdges(slots = groupExamState.todaySlots, onRefresh = groupExamViewModel::refresh)
    val continueFallbackTitle = stringResource(R.string.home_continue_exam_fallback)
    fun onContinueClick(attempt: UnfinishedAttemptSummary) {
        onContinueAttempt(attempt.attemptId, attempt.titleBn ?: continueFallbackTitle)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.feature_coming_soon)
    fun showComingSoon() {
        scope.launch { snackbarHostState.showSnackbar(comingSoon) }
    }
    val qbLabelRes = R.string.qm_question_bank
    val practiceLabelRes = R.string.qm_practice
    val groupLabelRes = R.string.qm_group
    val playgroundLabelRes = R.string.qm_playground
    val routineLabelRes = R.string.qm_routine
    fun onQuickMenuClick(item: QuickMenuItem) {
        when (item.labelRes) {
            qbLabelRes -> onNavigateToQB()
            practiceLabelRes -> onNavigateToPractice()
            groupLabelRes -> onNavigateToGroupList()
            playgroundLabelRes -> onNavigateToPlayground()
            routineLabelRes -> onNavigateToRoutine()
            else -> showComingSoon()
        }
    }
    // Only the live-exam card is left in upcomingExams; today's group exam has its own real card.
    fun onUpcomingCardClick(@Suppress("UNUSED_PARAMETER") card: ExamPreviewCard) = onNavigateToLiveExamList()

    // Compact: one scrolling column (phones). Medium/expanded: a genuine two-pane dashboard —
    // live/upcoming content on the left, glanceable generic data (quick menu, leaderboard) on
    // the right — instead of stretching a single column and padding out the extra width.
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val useTwoPane = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = ::showComingSoon,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.alpha(0.55f)
            ) {
                Icon(Icons.Filled.SmartToy, contentDescription = stringResource(R.string.ai_chat_coming_soon))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HomeHeaderRow(
                state = state,
                hasUnreadNotifications = hasUnreadNotifications,
                onNotificationsClick = dropUnlessResumed { onNavigateToNotifications() },
                onProfileClick = dropUnlessResumed { onNavigateToProfile() }
            )

            if (useTwoPane) {
                Row(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(end = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        QuickMenuSection(
                            onItemClick = ::onQuickMenuClick,
                            iconSize = 60.dp,
                            hiddenLabels = hiddenMenu
                        )
                        LeaderboardSection(state, onClick = ::showComingSoon)
                        Spacer(modifier = Modifier.padding(bottom = 12.dp))
                    }

                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        LiveAndUpcomingContent(
                            state = state,
                            onAction = ::showComingSoon,
                            onCardClick = ::onUpcomingCardClick,
                            groupExam = groupExam,
                            now = now,
                            onGroupExamClick = { onNavigateToGroupDetail(it) },
                            onContinueClick = ::onContinueClick
                        )
                        if (widgetPromo.visible) {
                            WidgetPromoCard(onAdd = widgetPromoViewModel::add, onClose = widgetPromoViewModel::dismiss)
                        }
                        Spacer(modifier = Modifier.padding(bottom = 12.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    LiveAndUpcomingContent(
                            state = state,
                            onAction = ::showComingSoon,
                            onCardClick = ::onUpcomingCardClick,
                            groupExam = groupExam,
                            now = now,
                            onGroupExamClick = { onNavigateToGroupDetail(it) },
                            onContinueClick = ::onContinueClick
                        )
                    if (widgetPromo.visible) {
                        WidgetPromoCard(
                            onAdd = widgetPromoViewModel::add,
                            onClose = widgetPromoViewModel::dismiss,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    QuickMenuSection(onItemClick = ::onQuickMenuClick, hiddenLabels = hiddenMenu)
                    LeaderboardSection(state, onClick = ::showComingSoon)
                    Spacer(modifier = Modifier.padding(bottom = 12.dp))
                }
            }
        }
    }
}

/** The running/upcoming-exam row plus the admission reminder and continue-progress cards. */
@Composable
private fun LiveAndUpcomingContent(
    state: HomeUiState,
    onAction: () -> Unit,
    onCardClick: (ExamPreviewCard) -> Unit,
    groupExam: HomeGroupExam?,
    now: Instant,
    onGroupExamClick: (groupId: String) -> Unit,
    onContinueClick: (UnfinishedAttemptSummary) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.upcomingExams.forEach { card -> ExamPreviewCardView(card, onClick = { onCardClick(card) }) }
        groupExam?.let { exam ->
            GroupExamPreviewCard(exam, now, onClick = dropUnlessResumed { exam.slot.groupId?.let(onGroupExamClick) })
        }
    }

    if (state.hasAdmissionReminder) {
        AdmissionReminderCard(
            titleRes = state.admissionTitleRes,
            subtitleRes = state.admissionSubtitleRes,
            onClick = onAction,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    state.continueAttempt?.let { attempt ->
        ContinueAttemptCard(
            attempt = attempt,
            remainingSeconds = state.continueRemainingSeconds,
            onClick = dropUnlessResumed { onContinueClick(attempt) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

/** Tags for the header's real-profile pieces — used by HomeScreenTest. */
const val HOME_GREETING_TAG = "home_greeting"
const val HOME_USERNAME_TAG = "home_username"
const val HOME_PROFILE_BUTTON_TAG = "home_profile_button"
const val HOME_BELL_TAG = "home_bell"
const val HOME_BELL_DOT_TAG = "home_bell_dot"

@Composable
private fun HomeHeaderRow(state: HomeUiState, hasUnreadNotifications: Boolean, onNotificationsClick: () -> Unit, onProfileClick: () -> Unit) {
    val profile = state.profile
    val fullName = profile?.fullName?.takeIf { it.isNotBlank() }
    val openProfile = stringResource(R.string.profile_open)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (fullName != null) stringResource(R.string.home_greeting, fullName)
                else stringResource(R.string.home_greeting_no_name),
                style = ProstutiTypography.bnTitleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(HOME_GREETING_TAG)
            )
            profile?.username?.takeIf { it.isNotBlank() }?.let { username ->
                Text(
                    text = stringResource(R.string.username_handle, username),
                    style = ProstutiTypography.enBodyMedium.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp).testTag(HOME_USERNAME_TAG)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
            ) {
                IconButton(onClick = onNotificationsClick, modifier = Modifier.testTag(HOME_BELL_TAG)) {
                    Icon(
                        Icons.Filled.NotificationsNone,
                        contentDescription = stringResource(if (hasUnreadNotifications) R.string.notif_bell_unread_cd else R.string.notifications),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (hasUnreadNotifications) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 10.dp).size(9.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape).testTag(HOME_BELL_DOT_TAG)
                    )
                }
            }
            UserAvatar(
                imageUrl = profile?.profileImage,
                name = profile?.firstName,
                contentDescription = null,
                size = 44.dp,
                modifier = Modifier
                    .testTag(HOME_PROFILE_BUTTON_TAG)
                    .clip(CircleShape)
                    .clickable(onClickLabel = openProfile, role = Role.Button, onClick = onProfileClick)
                    .semantics { contentDescription = openProfile }
            )
        }
    }
}
