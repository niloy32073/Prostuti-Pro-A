package com.prostuti.student.ui.screen.home

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
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.ui.screen.home.component.AdmissionReminderCard
import com.prostuti.student.ui.screen.home.component.ContinueProgressCard
import com.prostuti.student.ui.screen.home.component.ExamPreviewCardView
import com.prostuti.student.ui.screen.home.component.LeaderboardSection
import com.prostuti.student.ui.screen.home.component.QuickMenuItem
import com.prostuti.student.ui.screen.home.component.QuickMenuSection
import com.prostuti.student.ui.theme.MonoFontFamily
import com.prostuti.student.ui.theme.ProstutiTypography
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onNavigateToQB: () -> Unit = {},
    onNavigateToPractice: () -> Unit = {},
    onNavigateToGroupList: () -> Unit = {},
    onNavigateToLiveExamList: () -> Unit = {},
    onNavigateToGroupDetail: (groupId: String) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.feature_coming_soon)
    fun showComingSoon() {
        scope.launch { snackbarHostState.showSnackbar(comingSoon) }
    }
    val qbLabelRes = R.string.qm_question_bank
    val practiceLabelRes = R.string.qm_practice
    val groupLabelRes = R.string.qm_group
    fun onQuickMenuClick(item: QuickMenuItem) {
        when (item.labelRes) {
            qbLabelRes -> onNavigateToQB()
            practiceLabelRes -> onNavigateToPractice()
            groupLabelRes -> onNavigateToGroupList()
            else -> showComingSoon()
        }
    }
    // Mirrors ExamDummyData.GROUP_HSC27 — Home's own demo data (see HomeUiState) is still a
    // separate, hand-picked upcoming card, not fetched from the group repository.
    val demoGroupCardGroupId = "g_hsc27"
    fun onUpcomingCardClick(card: ExamPreviewCard) {
        if (card.isPrimary) onNavigateToLiveExamList() else onNavigateToGroupDetail(demoGroupCardGroupId)
    }

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
            HomeHeaderRow(state = state, onAction = ::showComingSoon)

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
                            iconSize = 60.dp
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
                        LiveAndUpcomingContent(state = state, onAction = ::showComingSoon, onCardClick = ::onUpcomingCardClick)
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
                    LiveAndUpcomingContent(state = state, onAction = ::showComingSoon, onCardClick = ::onUpcomingCardClick)
                    QuickMenuSection(onItemClick = ::onQuickMenuClick)
                    LeaderboardSection(state, onClick = ::showComingSoon)
                    Spacer(modifier = Modifier.padding(bottom = 12.dp))
                }
            }
        }
    }
}

/** The running/upcoming-exam row plus the admission reminder and continue-progress cards. */
@Composable
private fun LiveAndUpcomingContent(state: HomeUiState, onAction: () -> Unit, onCardClick: (ExamPreviewCard) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.upcomingExams.forEach { card -> ExamPreviewCardView(card, onClick = { onCardClick(card) }) }
    }

    if (state.hasAdmissionReminder) {
        AdmissionReminderCard(
            titleRes = state.admissionTitleRes,
            subtitleRes = state.admissionSubtitleRes,
            onClick = onAction,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    if (state.hasContinueProgress) {
        ContinueProgressCard(
            subtitleRes = state.continueSubtitleRes,
            percent = state.continuePercent,
            onClick = onAction,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HomeHeaderRow(state: HomeUiState, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = stringResource(state.greetingRes),
                style = ProstutiTypography.bnTitleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(state.batchStreamRes),
                style = ProstutiTypography.enBodyMedium.copy(fontFamily = MonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
            ) {
                IconButton(onClick = onAction) {
                    Icon(
                        Icons.Filled.NotificationsNone,
                        contentDescription = stringResource(R.string.notifications),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .clickable(onClick = onAction),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = stringResource(R.string.profile),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
