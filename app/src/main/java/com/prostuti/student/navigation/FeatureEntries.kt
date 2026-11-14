package com.prostuti.student.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.SupportingPaneSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberSupportingPaneSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.ui.screen.group.GroupDetailViewModel
import com.prostuti.student.ui.screen.group.GroupListViewModel
import com.prostuti.student.ui.screen.group.GroupMembersViewModel
import com.prostuti.student.ui.screen.group.GroupRoutineCalendarViewModel
import com.prostuti.student.ui.screen.group.component.AddMemberSheet
import com.prostuti.student.ui.screen.group.component.GroupCreateJoinSheet
import com.prostuti.student.ui.screen.routine.RoutineTabViewModel
import com.prostuti.student.ui.screen.routine.slot.SlotDetailSheet
import com.prostuti.student.ui.screen.routine.slot.rememberNow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategy
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.domain.notification.NotificationTarget
import com.prostuti.student.ui.component.AdaptiveWidthContainer
import com.prostuti.student.ui.component.BesideListPaneSized
import com.prostuti.student.ui.component.IconBadge
import com.prostuti.student.ui.component.PaneSized
import com.prostuti.student.ui.screen.analysis.AttemptAnalysisScreen
import com.prostuti.student.ui.screen.bookmark.BookmarkHistoryScreen
import com.prostuti.student.ui.screen.examengine.ANSWER_SHEET_NO_TOPIC
import com.prostuti.student.ui.screen.examengine.ExamAnswerSheetScreen
import com.prostuti.student.ui.screen.examengine.ExamAttemptScreen
import com.prostuti.student.ui.screen.examengine.ExamAttemptStore
import com.prostuti.student.ui.screen.examengine.ExamLeaderboardScreen
import com.prostuti.student.ui.screen.examengine.ExamResultScreen
import com.prostuti.student.ui.screen.examengine.LockedExamResultScreen
import com.prostuti.student.ui.screen.examengine.ResultLockGate
import com.prostuti.student.ui.screen.examengine.ResultPane
import com.prostuti.student.ui.screen.group.GroupDetailScreen
import com.prostuti.student.ui.screen.group.GroupJoinRequestsScreen
import com.prostuti.student.ui.screen.group.GroupListScreen
import com.prostuti.student.ui.screen.group.GroupMembersScreen
import com.prostuti.student.ui.screen.group.GroupPastExamsScreen
import com.prostuti.student.ui.screen.group.GroupRankingScreen
import com.prostuti.student.ui.screen.group.GroupRoutineCalendarScreen
import com.prostuti.student.ui.screen.group.GroupSettingsScreen
import com.prostuti.student.ui.screen.history.ExamHistoryScreen
import com.prostuti.student.ui.screen.live.LiveExamDetailScreen
import com.prostuti.student.ui.screen.live.LiveExamListScreen
import com.prostuti.student.ui.screen.notification.NotificationsScreen
import com.prostuti.student.ui.screen.playground.PlaygroundHubScreen
import com.prostuti.student.ui.screen.playground.graph.GraphBuilderScreen
import com.prostuti.student.ui.screen.playground.code.CodeVisualizerScreen
import com.prostuti.student.ui.screen.practice.PracticeChapterSelectScreen
import com.prostuti.student.ui.screen.practice.PracticeExamPropertiesScreen
import com.prostuti.student.ui.screen.practice.PracticeFlowViewModel
import com.prostuti.student.ui.screen.practice.PracticeScope
import com.prostuti.student.ui.screen.practice.PracticeSubjectChapterScreen
import com.prostuti.student.ui.screen.practice.PracticeSubjectSelectScreen
import com.prostuti.student.ui.screen.profile.ProfileScreen
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderMode
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderScreen
import com.prostuti.student.ui.screen.routine.builder.RoutineOwner
import com.prostuti.student.ui.screen.routine.builder.groupIdOrNull
import com.prostuti.student.ui.screen.timedexam.LockedResultScreen
import com.prostuti.student.ui.screen.timedexam.TimedAnswerSheetScreen
import com.prostuti.student.ui.screen.timedexam.TimedExamAttemptScreen
import com.prostuti.student.ui.screen.timedexam.WindowRankingScreen
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * The Main shell's scene strategies (the feature screens below live in the tabs' stacks):
 * - list-detail (groups, an exam result and its views): two panes from medium width;
 * - supporting pane (the practice builder's exam properties beside its picks): only when there is
 *   room for both — expanded width and at least medium height. Medium windows (a tablet upright) and a
 *   phone on its side get them as a screen of their own, after the combined subjects + chapters screen.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberMainSceneStrategies(windowAdaptiveInfo: WindowAdaptiveInfo): List<SceneStrategy<NavKey>> {
    val directive = remember(windowAdaptiveInfo) { calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(windowAdaptiveInfo) }
    // System Back steps back one screen (like the on-screen arrow). The library default keeps popping while
    // the panes look the same, and PopUntilCurrentDestinationChange while the pane role stays "detail" —
    // both take members → group → list → out of the flow in one press.
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        backNavigationBehavior = BackNavigationBehavior.PopUntilContentChange,
        directive = directive,
    )
    val practicePaneDirective = remember(windowAdaptiveInfo) {
        val wsc = windowAdaptiveInfo.windowSizeClass
        val roomForPane = wsc.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) &&
            wsc.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
        // Side by side only: the default directive would stack the two panes on a tall medium window.
        calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(
            maxHorizontalPartitions = if (roomForPane) 2 else 1,
            maxVerticalPartitions = 1,
        )
    }
    // Back closes the supporting pane first.
    val supportingPaneStrategy = rememberSupportingPaneSceneStrategy<NavKey>(
        backNavigationBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
        directive = practicePaneDirective,
    )
    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
    // Overlays (the bottom sheets) first: they sit on whatever scene the screens below make.
    return remember(listDetailStrategy, supportingPaneStrategy) { listOf(bottomSheetStrategy, listDetailStrategy, supportingPaneStrategy) }
}

/** A result screen shown right beside its exam result needs no back arrow. */
@Composable
private fun isBesideResult(stack: List<NavKey>, route: NavKey): Boolean =
    isShownBesideList() && stack.getOrNull(stack.lastIndexOf(route) - 1) is ExamResult

/**
 * A detail pane whose Back lands on the list beside it hides its arrow (the list is already on screen).
 * Deeper screens (members, calendar…) keep theirs: Back takes them to the previous detail.
 */
@Composable
private fun isRightBesideList(stack: List<NavKey>, route: NavKey, list: NavKey): Boolean =
    isShownBesideList() && stack.getOrNull(stack.lastIndexOf(route) - 1) == list

/**
 * Every feature screen. Each one is pushed onto the tab it was opened from ([Navigator.navigate]);
 * exams in progress are [FullScreenRoute]s (no bar or rail on any window size).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.featureEntries(navigator: Navigator, windowAdaptiveInfo: WindowAdaptiveInfo) {
    entry<PlaygroundHub> {
        PlaygroundHubScreen(
            onBack = { navigator.goBack() },
            onOpenGraphBuilder = { id -> navigator.navigate(GraphBuilder(id)) },
            onOpenCodeVisualizer = { navigator.navigate(CodeVisualizer) },
        )
    }
    entry<CodeVisualizer> {
        CodeVisualizerScreen(onBack = { navigator.goBack() })
    }
    entry<GraphBuilder> { route ->
        GraphBuilderScreen(
            onBack = { navigator.goBack() },
            viewModel = koinViewModel { parametersOf(route) }
        )
    }

    // ── Profile (GET auth/me) — lays itself out per window size class ──
    entry<Profile> {
        ProfileScreen(onBack = { navigator.goBack() })
    }

    // ── Bookmark history (MCQ bookmarks) — 1/2/3-column staggered grid by window size class ──
    entry<BookmarkHistory> {
        BookmarkHistoryScreen(onBack = { navigator.goBack() })
    }

    // ── Exam history (finished + missed attempts) → result / answer sheet / analysis ──
    entry<ExamHistory> {
        ExamHistoryScreen(
            onBack = { navigator.goBack() },
            onOpenAttempt = { item ->
                navigator.navigate(
                    ExamResult(
                        attemptId = item.attemptId,
                        titleBn = item.examTitleBn.ifBlank { item.examTitleEn },
                        fromHistory = true
                    )
                )
            }
        )
    }
    entry<AttemptAnalysis>(metadata = { route -> besideResultMetadata(route.besideResultOf) }) { route ->
        ResultLockGate(route.lockResultUntil, locked = { left ->
            LockedExamResultScreen(route.attemptId, route.titleBn, left, onBack = { navigator.goBack() })
        }) {
        BesideResultPane(route.besideResultOf) {
        AttemptAnalysisScreen(
            attemptId = route.attemptId,
            titleBn = route.titleBn,
            onBack = { navigator.goBack() },
            showBack = !isBesideResult(navigator.stackOf(route), route),
            onOpenTopic = { topicId, topicName ->
                navigator.navigate(
                    ExamAnswerSheet(
                        attemptId = route.attemptId,
                        titleBn = route.titleBn,
                        topicFilter = topicId ?: ANSWER_SHEET_NO_TOPIC,
                        topicName = topicName,
                        lockResultUntil = route.lockResultUntil,
                        // Stays in the same pane, on top of the analysis (Back returns to it).
                        besideResultOf = route.besideResultOf
                    )
                )
            }
        )
        }
        }
    }

    // ── Exam Engine (practice/live/group attempt + result/answer-sheet/leaderboard) ──
    entry<ExamAttempt> { route ->
        ExamAttemptScreen(
            attemptId = route.attemptId,
            onBack = { navigator.goBack() },
            onSubmitted = {
                navigator.replaceTop(ExamResult(attemptId = route.attemptId, titleBn = route.titleBn, lockResultUntil = route.lockResultUntil))
            },
            lockResultUntil = route.lockResultUntil
        )
    }
    // Medium and wider: the result stays on the left as a list and the answer sheet, topic
    // analysis or leaderboard opens on the right. Compact: one screen at a time, as before.
    // Each attempt gets its own scene key, so a retake's result never shares panes with the old one.
    entry<ExamResult>(
        metadata = { route ->
            ListDetailSceneStrategy.listPane(
                sceneKey = resultScene(route.attemptId),
                detailPlaceholder = { ResultDetailPlaceholder() }
            )
        }
    ) { route ->
        // Drop whatever is open beside this result, so a new pick replaces it instead of stacking.
        fun popToResult() = navigator.popUntil { it == route }
        fun openBeside(detail: MainRoute) {
            popToResult()
            navigator.navigate(detail)
        }
        val own = navigator.stackOf(route)
        val selectedPane = when (own.getOrNull(own.lastIndexOf(route) + 1)) {
            is ExamAnswerSheet -> ResultPane.ANSWER_SHEET
            is AttemptAnalysis -> ResultPane.ANALYSIS
            is ExamLeaderboardRoute -> ResultPane.LEADERBOARD
            else -> null
        }
        ResultLockGate(route.lockResultUntil, locked = { left ->
            LockedExamResultScreen(route.attemptId, route.titleBn, left, onBack = { navigator.goBack() })
        }) {
        ExamResultScreen(
            attemptId = route.attemptId,
            titleBn = route.titleBn,
            fromHistory = route.fromHistory,
            showPaneList = windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),
            selectedPane = selectedPane,
            onViewAnswerSheet = {
                openBeside(ExamAnswerSheet(route.attemptId, route.titleBn, lockResultUntil = route.lockResultUntil, besideResultOf = route.attemptId))
            },
            onViewAnalysis = {
                openBeside(AttemptAnalysis(route.attemptId, route.titleBn, route.lockResultUntil, besideResultOf = route.attemptId))
            },
            onViewLeaderboard = { examId ->
                openBeside(ExamLeaderboardRoute(examId = examId, titleBn = route.titleBn, besideResultOf = route.attemptId))
            },
            onRetakeStarted = { attempt ->
                ExamAttemptStore.put(attempt)
                popToResult()
                navigator.navigate(ExamAttempt(attemptId = attempt.attemptId, titleBn = route.titleBn))
            },
            onFinish = {
                if (route.fromHistory) {
                    popToResult()
                    navigator.goBack()
                } else {
                    navigator.popToRoot()
                }
            }
        )
        }
    }
    entry<ExamAnswerSheet>(metadata = { route -> besideResultMetadata(route.besideResultOf) }) { route ->
        ResultLockGate(route.lockResultUntil, locked = { left ->
            LockedExamResultScreen(route.attemptId, route.titleBn, left, onBack = { navigator.goBack() })
        }) {
        BesideResultPane(route.besideResultOf) {
        ExamAnswerSheetScreen(
            attemptId = route.attemptId,
            titleBn = route.titleBn,
            topicFilter = route.topicFilter,
            topicName = route.topicName,
            onBack = { navigator.goBack() },
            showBack = !isBesideResult(navigator.stackOf(route), route)
        )
        }
        }
    }
    entry<ExamLeaderboardRoute>(metadata = { route -> besideResultMetadata(route.besideResultOf) }) { route ->
        BesideResultPane(route.besideResultOf) {
            ExamLeaderboardScreen(
                examId = route.examId,
                titleBn = route.titleBn,
                onBack = { navigator.goBack() },
                showBack = !isBesideResult(navigator.stackOf(route), route)
            )
        }
    }

    // ── Practice Builder (subject -> chapter/topic -> properties -> Exam Engine) ──
    // Medium/Expanded combines the first two steps into one side-by-side screen
    // (SubjectChapterMedium/Expanded in the design) and skips straight to ExamProperties.
    // ── Practice builder ── its screens share the picks through PracticeFlowViewModel (FlowViewModelStore).
    // Compact: subjects → chapters → properties, three screens. Medium and wider: subjects and
    // chapters on one screen (the main pane). The properties open beside it as a supporting pane
    // when practicePaneDirective has room (Next again then just refreshes them), else as their own screen.
    entry<PracticeSubjectSelect>(
        metadata = FlowViewModelStore.root(PRACTICE_FLOW) + SupportingPaneSceneStrategy.mainPane(PRACTICE_SCENE)
    ) {
        if (windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            val flow = koinViewModel<PracticeFlowViewModel>(viewModelStoreOwner = LocalFlowViewModelStoreOwner.current)
            PracticeSubjectChapterScreen(
                onBack = { navigator.goBack() },
                onNext = { subjectIds, chapterIds, topicIds ->
                    flow.setScope(PracticeScope(subjectIds, chapterIds, topicIds))
                    navigator.navigate(PracticeExamProperties(besideSubjects = true))
                }
            )
        } else {
            PracticeSubjectSelectScreen(
                onBack = { navigator.goBack() },
                onNext = { subjectIds -> navigator.navigate(PracticeChapterSelect(subjectIds)) }
            )
        }
    }
    entry<PracticeChapterSelect>(metadata = FlowViewModelStore.member(PRACTICE_FLOW)) { route ->
        val flow = koinViewModel<PracticeFlowViewModel>(viewModelStoreOwner = LocalFlowViewModelStoreOwner.current)
        PracticeChapterSelectScreen(
            subjectIds = route.subjectIds,
            onBack = { navigator.goBack() },
            onNext = { chapterIds, topicIds ->
                flow.setScope(PracticeScope(route.subjectIds, chapterIds, topicIds))
                navigator.navigate(PracticeExamProperties())
            }
        )
    }
    entry<PracticeExamProperties>(
        metadata = { route ->
            FlowViewModelStore.member(PRACTICE_FLOW) + if (route.besideSubjects) {
                SupportingPaneSceneStrategy.supportingPane(PRACTICE_SCENE) +
                    SupportingPaneSceneStrategy.preferredPaneSize(width = 420.dp)
            } else {
                emptyMap()
            }
        }
    ) { route ->
        val flow = koinViewModel<PracticeFlowViewModel>(viewModelStoreOwner = LocalFlowViewModelStoreOwner.current)
        val scope by flow.scope.collectAsStateWithLifecycle()
        val current = scope
        if (current == null) {
            // The process was killed here: the picks are gone, so go back to the step that makes them.
            // Logged because this once left a blank screen in testing that could not be reproduced.
            LaunchedEffect(Unit) {
                val top = navigator.stackOf(route).lastOrNull()
                if (top == route) {
                    Timber.w("Practice picks lost: leaving exam properties (stack size %d)", navigator.stackOf(route).size)
                    navigator.goBack()
                } else {
                    Timber.w("Practice picks lost, but exam properties is not on top (top: %s)", top?.let { it::class.simpleName })
                }
            }
        } else {
            PracticeExamPropertiesScreen(
                scope = current,
                showBack = !isShownBesideMainPane(),
                onBack = { navigator.goBack() },
                onExamGenerated = { attemptId, titleBn ->
                    navigator.popToRoot()
                    navigator.navigate(ExamAttempt(attemptId = attemptId, titleBn = titleBn))
                }
            )
        }
    }

    // ── Group exam ── every screen lays itself out for the pane it gets (PaneSized): one column
    // when narrow, two or three side by side when wide (GROUP_ROUTINE_PLAN.md §10).
    // Leaving or deleting a group closes every open screen of it, back to wherever it was opened from.
    fun closeGroup(groupId: String) {
        navigator.removeEverywhere { key ->
            when (key) {
                is GroupDetail -> key.groupId == groupId
                is GroupSettings -> key.groupId == groupId
                is GroupMembers -> key.groupId == groupId
                is GroupJoinRequests -> key.groupId == groupId
                is GroupRoutineCalendar -> key.groupId == groupId
                is GroupPastExams -> key.groupId == groupId
                is GroupRanking -> key.groupId == groupId
                is RoutineBuilder -> key.owner.groupIdOrNull == groupId
                else -> false
            }
        }
    }
    // Medium and wider: the group list stays on the left and the open group (or its members,
    // calendar, ranking…) on the right. Compact: one screen at a time, as before.
    entry<GroupList>(
        metadata = ListDetailSceneStrategy.listPane(sceneKey = GROUP_SCENE, detailPlaceholder = { GroupDetailPlaceholder() }) +
            FlowViewModelStore.root(GROUPS_FLOW)
    ) {
        val openGroupId = (navigator.stackOf(GroupList).dropWhile { it != GroupList }.lastOrNull { it is GroupDetail } as? GroupDetail)?.groupId
        PaneSized {
            GroupListScreen(
                onBack = { navigator.goBack() },
                // Opening another group replaces whatever group screens were open beside the list.
                onNavigateToGroup = { groupId ->
                    navigator.popUntil { it == GroupList }
                    navigator.navigate(GroupDetail(groupId))
                },
                selectedGroupId = openGroupId,
                onOpenCreateJoin = { navigator.navigate(GroupCreateJoin) },
                viewModel = koinViewModel(viewModelStoreOwner = LocalFlowViewModelStoreOwner.current),
            )
        }
    }
    entry<GroupDetail>(
        metadata = { route -> ListDetailSceneStrategy.detailPane(GROUP_SCENE) + FlowViewModelStore.root(groupFlow(route.groupId)) }
    ) { route ->
        PaneSized {
            // Members' exam status, the routine calendar and past exams return on the real API in
            // P5–P7 (GROUP_ROUTINE_PLAN.md §6.5); admins already get the create-routine / one-off-exam card.
            GroupDetailScreen(
                groupId = route.groupId,
                onBack = { navigator.goBack() },
                onNavigateToMembers = { navigator.navigate(GroupMembers(route.groupId)) },
                onNavigateToSettings = { navigator.navigate(GroupSettings(route.groupId)) },
                onNavigateToJoinRequests = { navigator.navigate(GroupJoinRequests(route.groupId)) },
                onCreateRoutine = { navigator.navigate(RoutineBuilder(RoutineOwner.Group(route.groupId))) },
                onScheduleAdhoc = { navigator.navigate(RoutineBuilder(RoutineOwner.Group(route.groupId), RoutineBuilderMode.ADHOC)) },
                onOpenCalendar = { navigator.navigate(GroupRoutineCalendar(route.groupId)) },
                onOpenExamAttempt = { id, title, lock -> navigator.navigate(ExamAttempt(id, title, lock)) },
                onOpenExamResult = { id, title, lock -> navigator.navigate(ExamResult(id, title, fromHistory = true, lockResultUntil = lock)) },
                onOpenRanking = { examId, title, closes -> navigator.navigate(GroupRanking(route.groupId, examId, title, closes)) },
                onSeeAllPastExams = { navigator.navigate(GroupPastExams(route.groupId)) },
                onGroupClosed = { closeGroup(route.groupId) },
                showBack = !isRightBesideList(navigator.stackOf(route), route, GroupList),
                onShowSlot = { navigator.navigate(SlotDetail(SlotSheetHost.Group(route.groupId))) },
                viewModel = koinViewModel(
                    viewModelStoreOwner = LocalFlowViewModelStoreOwner.current,
                    parameters = { parametersOf(route.groupId) }
                ),
            )
        }
    }
    entry<GroupSettings>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
        PaneSized {
            GroupSettingsScreen(
                groupId = route.groupId,
                onBack = { navigator.goBack() },
                onOpenJoinRequests = { navigator.navigate(GroupJoinRequests(route.groupId)) },
                onOpenMembers = { navigator.navigate(GroupMembers(route.groupId)) },
                onGroupClosed = { closeGroup(route.groupId) }
            )
        }
    }
    entry<RoutineBuilder> { route ->
        PaneSized {
            RoutineBuilderScreen(owner = route.owner, mode = route.mode, onClose = { navigator.goBack() })
        }
    }
    entry<Notifications> {
        NotificationsScreen(
            onBack = { navigator.goBack() },
            onOpenTarget = { target ->
                when (target) {
                    is NotificationTarget.Group -> navigator.navigate(GroupDetail(target.groupId))
                    is NotificationTarget.JoinRequests -> navigator.navigate(GroupJoinRequests(target.groupId))
                    NotificationTarget.Routine -> {
                        navigator.goBack()
                        navigator.navigate(Routine)
                    }
                }
            }
        )
    }
    entry<GroupJoinRequests>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
        PaneSized { GroupJoinRequestsScreen(groupId = route.groupId, onBack = { navigator.goBack() }) }
    }
    entry<GroupRoutineCalendar>(
        metadata = { route -> ListDetailSceneStrategy.detailPane(GROUP_SCENE) + FlowViewModelStore.root(calendarFlow(route.groupId)) }
    ) { route ->
        PaneSized {
            GroupRoutineCalendarScreen(
                groupId = route.groupId,
                onBack = { navigator.goBack() },
                onScheduleAdhoc = { navigator.navigate(RoutineBuilder(RoutineOwner.Group(route.groupId), RoutineBuilderMode.ADHOC)) },
                onGroupClosed = { closeGroup(route.groupId) },
                onOpenExamAttempt = { id, title, lock -> navigator.navigate(ExamAttempt(id, title, lock)) },
                onOpenExamResult = { id, title, lock -> navigator.navigate(ExamResult(id, title, fromHistory = true, lockResultUntil = lock)) },
                onOpenRanking = { examId, title, closes -> navigator.navigate(GroupRanking(route.groupId, examId, title, closes)) },
                onShowSlot = { navigator.navigate(SlotDetail(SlotSheetHost.Calendar(route.groupId))) },
                viewModel = koinViewModel(
                    viewModelStoreOwner = LocalFlowViewModelStoreOwner.current,
                    parameters = { parametersOf(route.groupId) }
                ),
            )
        }
    }
    entry<GroupPastExams>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
        PaneSized {
            GroupPastExamsScreen(
                groupId = route.groupId,
                onBack = { navigator.goBack() },
                onOpenRanking = { slot, title -> slot.examId?.let { navigator.navigate(GroupRanking(route.groupId, it, title, slot.closesAt)) } }
            )
        }
    }
    entry<GroupRanking>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
        PaneSized {
            GroupRankingScreen(
                groupId = route.groupId, examId = route.examId, titleBn = route.titleBn, closesAt = route.closesAt,
                onBack = { navigator.goBack() },
                onOpenMyAnswerSheet = { id -> navigator.navigate(ExamAnswerSheet(id, route.titleBn, lockResultUntil = route.closesAt)) },
                onOpenMyResult = { id -> navigator.navigate(ExamResult(id, route.titleBn, fromHistory = true, lockResultUntil = route.closesAt)) },
            )
        }
    }
    entry<GroupMembers>(
        metadata = { route -> ListDetailSceneStrategy.detailPane(GROUP_SCENE) + FlowViewModelStore.root(membersFlow(route.groupId)) }
    ) { route ->
        PaneSized {
            GroupMembersScreen(
                groupId = route.groupId,
                onBack = { navigator.goBack() },
                onOpenAddMember = { navigator.navigate(AddGroupMember(route.groupId)) },
                viewModel = koinViewModel(
                    viewModelStoreOwner = LocalFlowViewModelStoreOwner.current,
                    parameters = { parametersOf(route.groupId) }
                ),
            )
        }
    }

    // ── Bottom sheets ── each reads the ViewModel of the screen below it through that screen's flow store.
    entry<GroupCreateJoin>(metadata = BottomSheetSceneStrategy.bottomSheet() + FlowViewModelStore.member(GROUPS_FLOW)) {
        val viewModel = koinViewModel<GroupListViewModel>(viewModelStoreOwner = LocalFlowViewModelStoreOwner.current)
        val state by viewModel.state.collectAsStateWithLifecycle()
        // Success sets navigateToGroupId: GroupList (below) then pops this sheet and opens the group.
        LaunchedEffect(Unit) { viewModel.openCreateJoinSheet() }
        DisposableEffect(Unit) { onDispose { viewModel.dismissCreateJoinSheet() } }
        GroupCreateJoinSheet(
            isSubmitting = state.isSubmittingCreateJoin,
            error = state.createJoinError,
            onCreate = viewModel::createGroup,
            onJoin = viewModel::joinGroup,
        )
    }
    entry<AddGroupMember>(
        metadata = { route -> BottomSheetSceneStrategy.bottomSheet() + FlowViewModelStore.member(membersFlow(route.groupId)) }
    ) { route ->
        val viewModel = koinViewModel<GroupMembersViewModel>(
            viewModelStoreOwner = LocalFlowViewModelStoreOwner.current,
            parameters = { parametersOf(route.groupId) }
        )
        val state by viewModel.state.collectAsStateWithLifecycle()
        SheetLifecycle(
            route = route, navigator = navigator, isOpen = state.showAddSheet,
            onOpen = viewModel::openAddSheet, onDismiss = viewModel::dismissAddSheet,
        )
        AddMemberSheet(isSubmitting = state.isAdding, error = state.addError, onAdd = viewModel::addMember)
    }
    entry<SlotDetail>(
        metadata = { route -> BottomSheetSceneStrategy.bottomSheet() + FlowViewModelStore.member(route.host.flowId) }
    ) { route ->
        val owner = LocalFlowViewModelStoreOwner.current
        val ticking by rememberNow()
        when (val host = route.host) {
            is SlotSheetHost.Group -> {
                val viewModel = koinViewModel<GroupDetailViewModel>(viewModelStoreOwner = owner, parameters = { parametersOf(host.groupId) })
                val state by viewModel.state.collectAsStateWithLifecycle()
                SlotSheetFor(
                    route = route, navigator = navigator, slot = state.openSlot, isOpen = state.openSlotId != null,
                    onDismiss = viewModel::dismissSlot,
                ) { slot ->
                    SlotDetailSheet(
                        slot = slot, label = state.labels[slot.id], now = ticking, canManage = state.isAdmin,
                        lastDate = if (slot.kind == SlotKind.ROUTINE) state.routineLastDate else null, isBusy = state.isSlotBusy,
                        onReschedule = viewModel::reschedule, onDelete = viewModel::deleteSlot,
                    )
                }
            }
            is SlotSheetHost.Calendar -> {
                val viewModel = koinViewModel<GroupRoutineCalendarViewModel>(viewModelStoreOwner = owner, parameters = { parametersOf(host.groupId) })
                val state by viewModel.state.collectAsStateWithLifecycle()
                SlotSheetFor(
                    route = route, navigator = navigator, slot = state.openSlot, isOpen = state.openSlotId != null,
                    onDismiss = viewModel::dismissSlot,
                ) { slot ->
                    SlotDetailSheet(
                        slot = slot, label = state.labels[slot.id], now = ticking, canManage = state.isAdmin,
                        lastDate = if (slot.kind == SlotKind.ROUTINE) state.routineEnd else null, isBusy = state.isSlotBusy,
                        onReschedule = viewModel::reschedule, onDelete = viewModel::deleteSlot,
                    )
                }
            }
            SlotSheetHost.RoutineTab -> {
                val viewModel = koinViewModel<RoutineTabViewModel>(viewModelStoreOwner = owner)
                val state by viewModel.state.collectAsStateWithLifecycle()
                SlotSheetFor(
                    route = route, navigator = navigator, slot = state.openSlot, isOpen = state.openSlotId != null,
                    onDismiss = viewModel::dismissSlot,
                ) { slot ->
                    SlotDetailSheet(
                        slot = slot, label = state.labels[slot.id], now = ticking, canManage = state.canManage(slot),
                        lastDate = if (slot.groupId == null) state.routineEnd else null, isBusy = state.isSlotBusy,
                        onReschedule = viewModel::reschedule, onDelete = viewModel::deleteSlot,
                    )
                }
            }
        }
    }

    // ── Live exam ──
    entry<LiveExamList> {
        LiveExamListScreen(
            onBack = { navigator.goBack() },
            onNavigateToDetail = { examId -> navigator.navigate(LiveExamDetail(examId)) }
        )
    }
    entry<LiveExamDetail> { route ->
        AdaptiveWidthContainer {
            LiveExamDetailScreen(
                liveExamId = route.liveExamId,
                onBack = { navigator.goBack() },
                onNavigateToAttempt = { examId -> navigator.navigate(TimedExamAttempt(examId, TimedExamKind.LIVE)) },
                onNavigateToLockedResult = { examId -> navigator.navigate(LockedResult(examId, TimedExamKind.LIVE)) },
                onNavigateToRanking = { examId -> navigator.navigate(WindowRanking(examId, TimedExamKind.LIVE)) }
            )
        }
    }

    // ── Shared timed-exam attempt/result/ranking/answer-sheet (live + group) ──
    entry<TimedExamAttempt> { route ->
        AdaptiveWidthContainer {
            TimedExamAttemptScreen(
                examId = route.examId, kind = route.kind,
                onSubmitted = { navigator.replaceTop(LockedResult(route.examId, route.kind)) }
            )
        }
    }
    entry<LockedResult> { route ->
        AdaptiveWidthContainer {
            LockedResultScreen(
                examId = route.examId, kind = route.kind,
                onBack = { navigator.goBack() },
                onNavigateToRanking = { examId -> navigator.replaceTop(WindowRanking(examId, route.kind)) }
            )
        }
    }
    entry<WindowRanking> { route ->
        AdaptiveWidthContainer {
            WindowRankingScreen(
                examId = route.examId, kind = route.kind,
                onBack = { navigator.goBack() },
                onNavigateToMyAnswerSheet = { navigator.navigate(TimedAnswerSheet(route.examId, route.kind)) }
            )
        }
    }
    entry<TimedAnswerSheet> { route ->
        AdaptiveWidthContainer {
            TimedAnswerSheetScreen(examId = route.examId, kind = route.kind, onBack = { navigator.goBack() })
        }
    }
}

/** One list-detail scene per exam result: the result on the left, the picked view on the right. */
private fun resultScene(attemptId: String) = "result:$attemptId"

/** Detail-pane metadata for a screen opened beside a result; none when it is a screen of its own. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun besideResultMetadata(besideResultOf: String?): Map<String, Any> =
    besideResultOf?.let { ListDetailSceneStrategy.detailPane(resultScene(it)) } ?: emptyMap()

/**
 * A screen opened from an exam result lays itself out for the right pane, not the whole window,
 * when it sits beside the result (medium and wider): large → 2 columns, medium → 1.
 */
@Composable
private fun BesideResultPane(besideResultOf: String?, content: @Composable () -> Unit) {
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    if (besideResultOf != null && wide) BesideListPaneSized(content) else content()
}

/** The right pane before the student picks a view of the result. */
@Composable
private fun ResultDetailPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconBadge(
            icon = Icons.Outlined.Insights,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = stringResource(R.string.result_pick_title),
            style = ProstutiTypography.bnTitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = stringResource(R.string.result_pick_body),
            style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** Flow id of the practice builder's shared store. */
private const val PRACTICE_FLOW = "practice"

/** The practice builder's picks (main pane) with its exam properties beside them (supporting pane). */
private const val PRACTICE_SCENE = "practice"

/** Groups and their screens share one list-detail scene, apart from Auth's. */
private const val GROUP_SCENE = "groups"

/** Flow stores that let a bottom sheet share the ViewModel of the screen it opened from. */
private const val GROUPS_FLOW = "groups"
private fun groupFlow(groupId: String) = "group:$groupId"
private fun calendarFlow(groupId: String) = "calendar:$groupId"
private fun membersFlow(groupId: String) = "members:$groupId"
const val ROUTINE_TAB_FLOW = "routine-tab"

/** The flow store of the screen whose ViewModel holds the open slot. */
private val SlotSheetHost.flowId: String
    get() = when (this) {
        is SlotSheetHost.Group -> groupFlow(groupId)
        is SlotSheetHost.Calendar -> calendarFlow(groupId)
        SlotSheetHost.RoutineTab -> ROUTINE_TAB_FLOW
    }

/**
 * A sheet whose screen below says whether it is open ([isOpen]): tells that screen it opened (once),
 * closes itself when the screen closes it (an action that succeeded), and tells the screen when the
 * student dismisses it.
 */
@Composable
private fun SheetLifecycle(route: NavKey, navigator: Navigator, isOpen: Boolean, onOpen: () -> Unit, onDismiss: () -> Unit) {
    // Close only on a real open → closed change: the collected state reaches us a frame after onOpen().
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { onOpen() }
    LaunchedEffect(isOpen) {
        if (isOpen) wasOpen = true
        else if (wasOpen && navigator.stackOf(route).lastOrNull() == route) navigator.goBack()
    }
    DisposableEffect(Unit) { onDispose { onDismiss() } }
}

/**
 * The slot sheet: closes itself when the slot is gone (moved, deleted, or the ViewModel was recreated
 * empty after process death) and tells the screen below when the student dismisses it.
 */
@Composable
private fun SlotSheetFor(
    route: NavKey,
    navigator: Navigator,
    slot: RoutineSlot?,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    content: @Composable (RoutineSlot) -> Unit,
) {
    LaunchedEffect(isOpen) {
        if (!isOpen && navigator.stackOf(route).lastOrNull() == route) navigator.goBack()
    }
    DisposableEffect(Unit) { onDispose { onDismiss() } }
    slot?.let { content(it) }
}

/** The right pane before a group is picked. */
@Composable
private fun GroupDetailPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconBadge(
            icon = Icons.Filled.Groups,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = stringResource(R.string.group_pick_title),
            style = ProstutiTypography.bnTitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = stringResource(R.string.group_pick_body),
            style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Opens a link the app was launched with (a home screen widget tap) in its tab, with a real back stack
 * behind it (Back goes where it would have if the student had walked there). Runs once Main is up, so
 * a link that arrives while signed out waits for sign-in. An exam in progress is left alone, so a tap
 * on the home screen never loses an attempt.
 */
@Composable
fun OpenAppLinks(appLinks: AppLinkRequests, navigator: Navigator) {
    val pending by appLinks.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        val link = pending ?: return@LaunchedEffect
        appLinks.consume()
        if (navigator.anyOpen { it is FullScreenRoute }) return@LaunchedEffect
        when (link) {
            AppLink.Home -> navigator.navigate(Home)
            AppLink.LiveExams -> navigator.openInTab(Home, listOf(LiveExamList))
            is AppLink.LiveExam -> navigator.openInTab(Home, listOf(LiveExamList, LiveExamDetail(link.examId)))
            is AppLink.GroupCalendar ->
                navigator.openInTab(Home, listOf(GroupList, GroupDetail(link.groupId), GroupRoutineCalendar(link.groupId)))
            AppLink.RoutineTab -> navigator.navigate(Routine)
            AppLink.CreateRoutine -> navigator.openInTab(Routine, listOf(RoutineBuilder(RoutineOwner.Personal)))
            AppLink.LeaderboardTab -> navigator.navigate(Leaderboard)
        }
    }
}
