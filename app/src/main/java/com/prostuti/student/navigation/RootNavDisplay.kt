package com.prostuti.student.navigation

import com.prostuti.student.ui.component.PaneSized
import com.prostuti.student.ui.component.BesideListPaneSized
import androidx.compose.material.icons.filled.Groups
import com.prostuti.student.ui.screen.notification.NotificationsScreen
import com.prostuti.student.domain.notification.NotificationTarget
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.ui.component.AdaptiveWidthContainer
import com.prostuti.student.ui.component.IconBadge
import com.prostuti.student.ui.screen.auth.AuthLoginScreen
import com.prostuti.student.ui.screen.auth.AuthResetNewPasswordScreen
import com.prostuti.student.ui.screen.auth.AuthResetRequestScreen
import com.prostuti.student.ui.screen.auth.AuthSignupScreen
import com.prostuti.student.ui.screen.auth.AuthStartScreen
import com.prostuti.student.ui.screen.auth.AuthVerifyOtpScreen
import com.prostuti.student.ui.screen.analysis.AttemptAnalysisScreen
import com.prostuti.student.ui.screen.examengine.ANSWER_SHEET_NO_TOPIC
import com.prostuti.student.ui.screen.examengine.ExamAnswerSheetScreen
import com.prostuti.student.ui.screen.examengine.ExamAttemptStore
import com.prostuti.student.ui.screen.history.ExamHistoryScreen
import com.prostuti.student.ui.screen.examengine.ExamAttemptScreen
import com.prostuti.student.ui.screen.examengine.ExamLeaderboardScreen
import com.prostuti.student.ui.screen.examengine.ExamResultScreen
import com.prostuti.student.ui.screen.examengine.ResultPane
import androidx.compose.material.icons.outlined.Insights
import com.prostuti.student.ui.screen.group.GroupDetailScreen
import com.prostuti.student.ui.screen.group.GroupJoinRequestsScreen
import com.prostuti.student.ui.screen.group.GroupSettingsScreen
import com.prostuti.student.ui.screen.group.GroupRankingScreen
import com.prostuti.student.ui.screen.examengine.LockedExamResultScreen
import com.prostuti.student.ui.screen.examengine.ResultLockGate
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderMode
import com.prostuti.student.ui.screen.routine.builder.RoutineBuilderScreen
import com.prostuti.student.ui.screen.routine.builder.RoutineOwner
import com.prostuti.student.ui.screen.routine.builder.groupIdOrNull
import com.prostuti.student.ui.screen.group.GroupListScreen
import com.prostuti.student.ui.screen.group.GroupMembersScreen
import com.prostuti.student.ui.screen.group.GroupPastExamsScreen
import com.prostuti.student.ui.screen.group.GroupRoutineCalendarScreen
import com.prostuti.student.ui.screen.intro.OnboardingScreen
import com.prostuti.student.ui.screen.live.LiveExamDetailScreen
import com.prostuti.student.ui.screen.live.LiveExamListScreen
import com.prostuti.student.ui.screen.practice.PracticeChapterSelectScreen
import com.prostuti.student.ui.screen.practice.PracticeExamPropertiesScreen
import com.prostuti.student.ui.screen.practice.PracticeSubjectChapterScreen
import com.prostuti.student.ui.screen.practice.PracticeSubjectSelectScreen
import com.prostuti.student.ui.screen.playground.PlaygroundHubScreen
import com.prostuti.student.ui.screen.playground.graph.GraphBuilderScreen
import com.prostuti.student.ui.screen.profile.ProfileScreen
import com.prostuti.student.ui.screen.bookmark.BookmarkHistoryScreen
import com.prostuti.student.ui.screen.timedexam.LockedResultScreen
import com.prostuti.student.ui.screen.timedexam.TimedAnswerSheetScreen
import com.prostuti.student.ui.screen.timedexam.TimedExamAttemptScreen
import com.prostuti.student.ui.screen.timedexam.WindowRankingScreen
import com.prostuti.student.ui.theme.ProstutiTypography
import org.koin.core.parameter.parametersOf
import org.koin.compose.viewmodel.koinViewModel
import androidx.window.core.layout.WindowSizeClass

/**
 * The Auth sub-graph (Auth → SignIn/SignUp → VerifyOtp/ResetRequest → ResetNewPassword) opts into
 * [ListDetailSceneStrategy]: on compact width the strategy declines (per its own width check) and
 * [NavDisplay] falls back to its default single-pane scene exactly as before — same push/pop
 * stack. On medium/expanded width it renders Auth pinned as the list pane with whichever step is
 * on top of the stack as the detail pane beside it, with no custom pane-splitting code here.
 *
 * [calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth] is used instead of the library's
 * plain default directive — the default only switches to two panes at Expanded width (≥840dp)
 * and stays single-pane on Medium (600–840dp), which is where most tablets in portrait land.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun RootNavDisplay(startDestination: RootRoute, innerPaddingValues: PaddingValues) {
    val backStack = rememberNavBackStack(startDestination)
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(windowAdaptiveInfo)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
    // A result screen shown right beside its exam result (medium and wider) needs no back arrow.
    fun isBesideResult(route: RootRoute): Boolean =
        windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
            backStack.getOrNull(backStack.lastIndexOf(route) - 1) is ExamResult

    OpenAppLinks(backStack)

    NavDisplay(
        modifier = Modifier.fillMaxSize().padding(innerPaddingValues),
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        sceneStrategies = listOf(listDetailStrategy),
        // Never pop the last entry — it would hand NavDisplay an empty backstack and crash.
        // Intro and Main are each, at times, the sole entry; Auth guards itself with its own
        // BackHandler (exit-confirmation dialog) before a hardware back press gets here.
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Intro> {
                OnboardingScreen(onFinished = {
                    backStack.clear()
                    backStack.add(Auth)
                })
            }
            entry<Auth>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { AuthDetailPlaceholder() }
                )
            ) {
                AuthStartScreen(
                    onNavigateToLogin = { identifier, firstName ->
                        backStack.add(SignIn(identifier, firstName))
                    },
                    onNavigateToSignup = { identifier -> backStack.add(SignUp(identifier)) }
                )
            }
            entry<SignIn>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthLoginScreen(
                    identifier = route.identifier,
                    firstName = route.firstName,
                    onBack = { backStack.removeLastOrNull() },
                    onForgotPassword = { id -> backStack.add(ResetRequest(id)) },
                    onSignedIn = {
                        backStack.clear()
                        backStack.add(Main)
                    }
                )
            }
            entry<SignUp>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthSignupScreen(
                    identifier = route.identifier,
                    onBack = { backStack.removeLastOrNull() },
                    onSignedUp = { identifier -> backStack.add(VerifyOtp(identifier)) }
                )
            }
            entry<VerifyOtp>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthVerifyOtpScreen(
                    identifier = route.identifier,
                    onBack = { backStack.removeLastOrNull() },
                    onVerified = {
                        backStack.clear()
                        backStack.add(Main)
                    }
                )
            }
            entry<ResetRequest>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthResetRequestScreen(
                    identifier = route.identifier,
                    onBack = { backStack.removeLastOrNull() },
                    onCodeEntered = { code -> backStack.add(ResetNewPassword(route.identifier, code)) }
                )
            }
            entry<ResetNewPassword>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthResetNewPasswordScreen(
                    identifier = route.identifier,
                    code = route.code,
                    onBack = { backStack.removeLastOrNull() },
                    // The reset revokes every session and returns no tokens: back to Sign in.
                    onPasswordReset = { backStack.removeAll { it is ResetRequest || it is ResetNewPassword } }
                )
            }
            entry<Main> {
                MainNavDisplay(
                    onLoggedOut = {
                        backStack.clear()
                        backStack.add(Auth)
                    },
                    onNavigateToExamEngine = { backStack.add(it) },
                    onNavigateToPractice = { backStack.add(PracticeSubjectSelect) },
                    onNavigateToGroupList = { backStack.add(GroupList) },
                    onNavigateToLiveExamList = { backStack.add(LiveExamList) },
                    onNavigateToGroupDetail = { groupId -> backStack.add(GroupDetail(groupId)) },
                    onNavigateToProfile = { backStack.add(Profile) },
                    onNavigateToPlayground = { backStack.add(PlaygroundHub) },
                    onNavigateToBookmarks = { backStack.add(BookmarkHistory) },
                    onNavigateToExamHistory = { backStack.add(ExamHistory) },
                    onOpenRoute = { backStack.add(it) }
                )
            }
            entry<PlaygroundHub> {
                PlaygroundHubScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onOpenGraphBuilder = { id -> backStack.add(GraphBuilder(id)) }
                )
            }
            entry<GraphBuilder> { route ->
                GraphBuilderScreen(
                    onBack = { backStack.removeLastOrNull() },
                    viewModel = koinViewModel { parametersOf(route) }
                )
            }

            // ── Profile (GET auth/me) — lays itself out per window size class ──
            entry<Profile> {
                ProfileScreen(onBack = { backStack.removeLastOrNull() })
            }

            // ── Bookmark history (MCQ bookmarks) — 1/2/3-column staggered grid by window size class ──
            entry<BookmarkHistory> {
                BookmarkHistoryScreen(onBack = { backStack.removeLastOrNull() })
            }

            // ── Exam history (finished + missed attempts) → result / answer sheet / analysis ──
            entry<ExamHistory> {
                ExamHistoryScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onOpenAttempt = { item ->
                        backStack.add(
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
                    LockedExamResultScreen(route.attemptId, route.titleBn, left, onBack = { backStack.removeLastOrNull() })
                }) {
                BesideResultPane(route.besideResultOf) {
                AttemptAnalysisScreen(
                    attemptId = route.attemptId,
                    titleBn = route.titleBn,
                    onBack = { backStack.removeLastOrNull() },
                    showBack = !isBesideResult(route),
                    onOpenTopic = { topicId, topicName ->
                        backStack.add(
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
                    onBack = { backStack.removeLastOrNull() },
                    onSubmitted = {
                        backStack.removeLastOrNull()
                        backStack.add(ExamResult(attemptId = route.attemptId, titleBn = route.titleBn, lockResultUntil = route.lockResultUntil))
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
                fun popToResult() {
                    while (backStack.size > 1 && backStack.last() != route) backStack.removeLastOrNull()
                }
                fun openBeside(detail: RootRoute) {
                    popToResult()
                    backStack.add(detail)
                }
                val selectedPane = when (backStack.getOrNull(backStack.lastIndexOf(route) + 1)) {
                    is ExamAnswerSheet -> ResultPane.ANSWER_SHEET
                    is AttemptAnalysis -> ResultPane.ANALYSIS
                    is ExamLeaderboardRoute -> ResultPane.LEADERBOARD
                    else -> null
                }
                ResultLockGate(route.lockResultUntil, locked = { left ->
                    LockedExamResultScreen(route.attemptId, route.titleBn, left, onBack = { backStack.removeLastOrNull() })
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
                        backStack.add(ExamAttempt(attemptId = attempt.attemptId, titleBn = route.titleBn))
                    },
                    onFinish = {
                        if (route.fromHistory) {
                            popToResult()
                            backStack.removeLastOrNull()
                        } else {
                            backStack.clear()
                            backStack.add(Main)
                        }
                    }
                )
                }
            }
            entry<ExamAnswerSheet>(metadata = { route -> besideResultMetadata(route.besideResultOf) }) { route ->
                ResultLockGate(route.lockResultUntil, locked = { left ->
                    LockedExamResultScreen(route.attemptId, route.titleBn, left, onBack = { backStack.removeLastOrNull() })
                }) {
                BesideResultPane(route.besideResultOf) {
                ExamAnswerSheetScreen(
                    attemptId = route.attemptId,
                    titleBn = route.titleBn,
                    topicFilter = route.topicFilter,
                    topicName = route.topicName,
                    onBack = { backStack.removeLastOrNull() },
                    showBack = !isBesideResult(route)
                )
                }
                }
            }
            entry<ExamLeaderboardRoute>(metadata = { route -> besideResultMetadata(route.besideResultOf) }) { route ->
                BesideResultPane(route.besideResultOf) {
                    ExamLeaderboardScreen(
                        examId = route.examId,
                        titleBn = route.titleBn,
                        onBack = { backStack.removeLastOrNull() },
                        showBack = !isBesideResult(route)
                    )
                }
            }

            // ── Practice Builder (subject -> chapter/topic -> properties -> Exam Engine) ──
            // Medium/Expanded combines the first two steps into one side-by-side screen
            // (SubjectChapterMedium/Expanded in the design) and skips straight to ExamProperties.
            entry<PracticeSubjectSelect> {
                if (windowAdaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
                    PracticeSubjectChapterScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onNext = { subjectIds, chapterIds, topicIds ->
                            backStack.add(PracticeExamProperties(subjectIds, chapterIds, topicIds))
                        }
                    )
                } else {
                    PracticeSubjectSelectScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onNext = { subjectIds -> backStack.add(PracticeChapterSelect(subjectIds)) }
                    )
                }
            }
            entry<PracticeChapterSelect> { route ->
                PracticeChapterSelectScreen(
                    subjectIds = route.subjectIds,
                    onBack = { backStack.removeLastOrNull() },
                    onNext = { chapterIds, topicIds ->
                        backStack.add(PracticeExamProperties(route.subjectIds, chapterIds, topicIds))
                    }
                )
            }
            entry<PracticeExamProperties> { route ->
                PracticeExamPropertiesScreen(
                    subjectIds = route.subjectIds,
                    chapterIds = route.chapterIds,
                    topicIds = route.topicIds,
                    onBack = { backStack.removeLastOrNull() },
                    onExamGenerated = { attemptId, titleBn ->
                        backStack.clear()
                        backStack.add(Main)
                        backStack.add(ExamAttempt(attemptId = attemptId, titleBn = titleBn))
                    }
                )
            }

            // ── Group exam ── every screen lays itself out for the pane it gets (PaneSized): one column
            // when narrow, two or three side by side when wide (GROUP_ROUTINE_PLAN.md §10).
            // Leaving or deleting a group closes every open screen of it, back to wherever it was opened from.
            val mainTabRequests: MainTabRequests = koinInject()
            fun closeGroup(groupId: String) {
                backStack.removeAll { key ->
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
                metadata = ListDetailSceneStrategy.listPane(sceneKey = GROUP_SCENE, detailPlaceholder = { GroupDetailPlaceholder() })
            ) {
                val openGroupId = (backStack.dropWhile { it != GroupList }.lastOrNull { it is GroupDetail } as? GroupDetail)?.groupId
                PaneSized {
                    GroupListScreen(
                        onBack = { backStack.removeLastOrNull() },
                        // Opening another group replaces whatever group screens were open beside the list.
                        onNavigateToGroup = { groupId ->
                            while (backStack.size > 1 && backStack.last() != GroupList) backStack.removeLastOrNull()
                            backStack.add(GroupDetail(groupId))
                        },
                        selectedGroupId = openGroupId,
                    )
                }
            }
            entry<GroupDetail>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
                PaneSized {
                    // Members' exam status, the routine calendar and past exams return on the real API in
                    // P5–P7 (GROUP_ROUTINE_PLAN.md §6.5); admins already get the create-routine / one-off-exam card.
                    GroupDetailScreen(
                        groupId = route.groupId,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToMembers = { backStack.add(GroupMembers(route.groupId)) },
                        onNavigateToSettings = { backStack.add(GroupSettings(route.groupId)) },
                        onNavigateToJoinRequests = { backStack.add(GroupJoinRequests(route.groupId)) },
                        onCreateRoutine = { backStack.add(RoutineBuilder(RoutineOwner.Group(route.groupId))) },
                        onScheduleAdhoc = { backStack.add(RoutineBuilder(RoutineOwner.Group(route.groupId), RoutineBuilderMode.ADHOC)) },
                        onOpenCalendar = { backStack.add(GroupRoutineCalendar(route.groupId)) },
                        onOpenExamAttempt = { id, title, lock -> backStack.add(ExamAttempt(id, title, lock)) },
                        onOpenExamResult = { id, title, lock -> backStack.add(ExamResult(id, title, fromHistory = true, lockResultUntil = lock)) },
                        onOpenRanking = { examId, title, closes -> backStack.add(GroupRanking(route.groupId, examId, title, closes)) },
                        onSeeAllPastExams = { backStack.add(GroupPastExams(route.groupId)) },
                        onGroupClosed = { closeGroup(route.groupId) }
                    )
                }
            }
            entry<GroupSettings>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
                PaneSized {
                    GroupSettingsScreen(
                        groupId = route.groupId,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenJoinRequests = { backStack.add(GroupJoinRequests(route.groupId)) },
                        onOpenMembers = { backStack.add(GroupMembers(route.groupId)) },
                        onGroupClosed = { closeGroup(route.groupId) }
                    )
                }
            }
            entry<RoutineBuilder> { route ->
                PaneSized {
                    RoutineBuilderScreen(owner = route.owner, mode = route.mode, onClose = { backStack.removeLastOrNull() })
                }
            }
            entry<Notifications> {
                NotificationsScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onOpenTarget = { target ->
                        when (target) {
                            is NotificationTarget.Group -> backStack.add(GroupDetail(target.groupId))
                            is NotificationTarget.JoinRequests -> backStack.add(GroupJoinRequests(target.groupId))
                            NotificationTarget.Routine -> {
                                backStack.removeLastOrNull()
                                mainTabRequests.request(Routine)
                            }
                        }
                    }
                )
            }
            entry<GroupJoinRequests>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
                PaneSized { GroupJoinRequestsScreen(groupId = route.groupId, onBack = { backStack.removeLastOrNull() }) }
            }
            entry<GroupRoutineCalendar>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
                PaneSized {
                    GroupRoutineCalendarScreen(
                        groupId = route.groupId,
                        onBack = { backStack.removeLastOrNull() },
                        onScheduleAdhoc = { backStack.add(RoutineBuilder(RoutineOwner.Group(route.groupId), RoutineBuilderMode.ADHOC)) },
                        onGroupClosed = { closeGroup(route.groupId) },
                        onOpenExamAttempt = { id, title, lock -> backStack.add(ExamAttempt(id, title, lock)) },
                        onOpenExamResult = { id, title, lock -> backStack.add(ExamResult(id, title, fromHistory = true, lockResultUntil = lock)) },
                        onOpenRanking = { examId, title, closes -> backStack.add(GroupRanking(route.groupId, examId, title, closes)) },
                    )
                }
            }
            entry<GroupPastExams>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
                PaneSized {
                    GroupPastExamsScreen(
                        groupId = route.groupId,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenRanking = { slot, title -> slot.examId?.let { backStack.add(GroupRanking(route.groupId, it, title, slot.closesAt)) } }
                    )
                }
            }
            entry<GroupRanking>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
                PaneSized {
                    GroupRankingScreen(
                        groupId = route.groupId, examId = route.examId, titleBn = route.titleBn, closesAt = route.closesAt,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenMyAnswerSheet = { id -> backStack.add(ExamAnswerSheet(id, route.titleBn, lockResultUntil = route.closesAt)) },
                        onOpenMyResult = { id -> backStack.add(ExamResult(id, route.titleBn, fromHistory = true, lockResultUntil = route.closesAt)) },
                    )
                }
            }
            entry<GroupMembers>(metadata = ListDetailSceneStrategy.detailPane(GROUP_SCENE)) { route ->
                PaneSized { GroupMembersScreen(groupId = route.groupId, onBack = { backStack.removeLastOrNull() }) }
            }

            // ── Live exam ──
            entry<LiveExamList> {
                LiveExamListScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToDetail = { examId -> backStack.add(LiveExamDetail(examId)) }
                )
            }
            entry<LiveExamDetail> { route ->
                AdaptiveWidthContainer {
                    LiveExamDetailScreen(
                        liveExamId = route.liveExamId,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToAttempt = { examId -> backStack.add(TimedExamAttempt(examId, TimedExamKind.LIVE)) },
                        onNavigateToLockedResult = { examId -> backStack.add(LockedResult(examId, TimedExamKind.LIVE)) },
                        onNavigateToRanking = { examId -> backStack.add(WindowRanking(examId, TimedExamKind.LIVE)) }
                    )
                }
            }

            // ── Shared timed-exam attempt/result/ranking/answer-sheet (live + group) ──
            entry<TimedExamAttempt> { route ->
                AdaptiveWidthContainer {
                    TimedExamAttemptScreen(
                        examId = route.examId, kind = route.kind,
                        onSubmitted = {
                            backStack.removeLastOrNull()
                            backStack.add(LockedResult(route.examId, route.kind))
                        }
                    )
                }
            }
            entry<LockedResult> { route ->
                AdaptiveWidthContainer {
                    LockedResultScreen(
                        examId = route.examId, kind = route.kind,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToRanking = { examId ->
                            backStack.removeLastOrNull()
                            backStack.add(WindowRanking(examId, route.kind))
                        }
                    )
                }
            }
            entry<WindowRanking> { route ->
                AdaptiveWidthContainer {
                    WindowRankingScreen(
                        examId = route.examId, kind = route.kind,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToMyAnswerSheet = { backStack.add(TimedAnswerSheet(route.examId, route.kind)) }
                    )
                }
            }
            entry<TimedAnswerSheet> { route ->
                AdaptiveWidthContainer {
                    TimedAnswerSheetScreen(examId = route.examId, kind = route.kind, onBack = { backStack.removeLastOrNull() })
                }
            }
        }
    )
}

/** Shown beside AuthStart on medium/expanded width before the user picks a login/signup step. */
@Composable
private fun AuthDetailPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconBadge(
            icon = Icons.Filled.School,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = stringResource(R.string.auth_detail_placeholder_title),
            style = ProstutiTypography.bnTitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = stringResource(R.string.auth_detail_placeholder_subtitle),
            style = ProstutiTypography.bnBodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
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

/** Groups and their screens share one list-detail scene, apart from Auth's. */
private const val GROUP_SCENE = "groups"

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
 * Opens a link the app was launched with (a home screen widget tap) on top of Main, with a real
 * back stack behind it (Back goes where it would have if the user had walked there). Signed out
 * there is no Main: the app already shows sign-in, which is all a link can ask for then. An exam in
 * progress is left alone, so a tap on the home screen never loses an attempt.
 */
@Composable
private fun OpenAppLinks(backStack: NavBackStack<NavKey>) {
    val appLinks: AppLinkRequests = koinInject()
    val tabRequests: MainTabRequests = koinInject()
    val pending by appLinks.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        val link = pending ?: return@LaunchedEffect
        appLinks.consume()
        if (Main !in backStack) return@LaunchedEffect
        if (backStack.any { it is ExamAttempt || it is TimedExamAttempt }) return@LaunchedEffect
        while (backStack.last() != Main) backStack.removeAt(backStack.lastIndex)
        when (link) {
            AppLink.Home -> Unit
            AppLink.LiveExams -> backStack.add(LiveExamList)
            is AppLink.LiveExam -> backStack.addAll(listOf(LiveExamList, LiveExamDetail(link.examId)))
            is AppLink.GroupCalendar -> backStack.addAll(listOf(GroupList, GroupDetail(link.groupId), GroupRoutineCalendar(link.groupId)))
            AppLink.RoutineTab -> tabRequests.request(Routine)
            AppLink.CreateRoutine -> {
                tabRequests.request(Routine)
                backStack.add(RoutineBuilder(RoutineOwner.Personal))
            }
            AppLink.LeaderboardTab -> tabRequests.request(Leaderboard)
        }
    }
}
