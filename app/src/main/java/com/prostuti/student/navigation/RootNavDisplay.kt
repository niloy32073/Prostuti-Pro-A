package com.prostuti.student.navigation

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
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
import com.prostuti.student.ui.screen.examengine.ExamAnswerSheetScreen
import com.prostuti.student.ui.screen.examengine.ExamAttemptScreen
import com.prostuti.student.ui.screen.examengine.ExamLeaderboardScreen
import com.prostuti.student.ui.screen.examengine.ExamResultScreen
import com.prostuti.student.ui.screen.group.GroupDetailScreen
import com.prostuti.student.ui.screen.group.GroupListScreen
import com.prostuti.student.ui.screen.group.GroupMembersScreen
import com.prostuti.student.ui.screen.group.GroupPastExamsScreen
import com.prostuti.student.ui.screen.group.GroupRoutineCalendarScreen
import com.prostuti.student.ui.screen.group.GroupRoutineChapterScreen
import com.prostuti.student.ui.screen.group.GroupRoutineSubjectScreen
import com.prostuti.student.ui.screen.group.GroupRoutineWindowScreen
import com.prostuti.student.ui.screen.intro.OnboardingScreen
import com.prostuti.student.ui.screen.live.LiveExamDetailScreen
import com.prostuti.student.ui.screen.live.LiveExamListScreen
import com.prostuti.student.ui.screen.practice.PracticeChapterSelectScreen
import com.prostuti.student.ui.screen.practice.PracticeExamPropertiesScreen
import com.prostuti.student.ui.screen.practice.PracticeSubjectChapterScreen
import com.prostuti.student.ui.screen.practice.PracticeSubjectSelectScreen
import com.prostuti.student.ui.screen.timedexam.LockedResultScreen
import com.prostuti.student.ui.screen.timedexam.TimedAnswerSheetScreen
import com.prostuti.student.ui.screen.timedexam.TimedExamAttemptScreen
import com.prostuti.student.ui.screen.timedexam.WindowRankingScreen
import com.prostuti.student.ui.theme.ProstutiTypography
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
                    onVerified = { resetToken -> backStack.add(ResetNewPassword(resetToken)) }
                )
            }
            entry<ResetNewPassword>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthResetNewPasswordScreen(
                    resetToken = route.resetToken,
                    onBack = { backStack.removeLastOrNull() },
                    onPasswordReset = {
                        backStack.clear()
                        backStack.add(Main)
                    }
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
                    onNavigateToGroupDetail = { groupId -> backStack.add(GroupDetail(groupId)) }
                )
            }

            // ── Exam Engine (practice/live/group attempt + result/answer-sheet/leaderboard) ──
            entry<ExamAttempt> { route ->
                ExamAttemptScreen(
                    attemptId = route.attemptId,
                    onBack = { backStack.removeLastOrNull() },
                    onSubmitted = {
                        backStack.removeLastOrNull()
                        backStack.add(ExamResult(attemptId = route.attemptId, titleBn = route.titleBn))
                    }
                )
            }
            entry<ExamResult> { route ->
                ExamResultScreen(
                    attemptId = route.attemptId,
                    titleBn = route.titleBn,
                    onViewAnswerSheet = { backStack.add(ExamAnswerSheet(route.attemptId, route.titleBn)) },
                    onViewLeaderboard = { examId ->
                        backStack.add(ExamLeaderboardRoute(examId = examId, titleBn = route.titleBn))
                    },
                    onFinish = {
                        backStack.clear()
                        backStack.add(Main)
                    }
                )
            }
            entry<ExamAnswerSheet> { route ->
                ExamAnswerSheetScreen(
                    attemptId = route.attemptId,
                    titleBn = route.titleBn,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<ExamLeaderboardRoute> { route ->
                ExamLeaderboardScreen(
                    examId = route.examId,
                    titleBn = route.titleBn,
                    onBack = { backStack.removeLastOrNull() }
                )
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

            // ── Group exam ── every entry below is wrapped in AdaptiveWidthContainer: none of
            // these screens has its own tablet design, so centering the phone layout with a
            // capped width on medium/expanded windows is their whole "adaptive" story (P7).
            entry<GroupList> {
                AdaptiveWidthContainer {
                    GroupListScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToGroup = { groupId -> backStack.add(GroupDetail(groupId)) }
                    )
                }
            }
            entry<GroupDetail> { route ->
                AdaptiveWidthContainer {
                    GroupDetailScreen(
                        groupId = route.groupId,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToRoutineCreate = { backStack.add(GroupRoutineSubjectSelect(route.groupId)) },
                        onNavigateToCalendar = { backStack.add(GroupRoutineCalendar(route.groupId)) },
                        onNavigateToPastExams = { backStack.add(GroupPastExams(route.groupId)) },
                        onNavigateToMembers = { backStack.add(GroupMembers(route.groupId)) },
                        onNavigateToPastExam = { examId -> backStack.add(WindowRanking(examId, TimedExamKind.GROUP)) },
                        onNavigateToAttempt = { examId -> backStack.add(TimedExamAttempt(examId, TimedExamKind.GROUP)) },
                        onNavigateToLockedResult = { examId -> backStack.add(LockedResult(examId, TimedExamKind.GROUP)) },
                        onNavigateToRanking = { examId -> backStack.add(WindowRanking(examId, TimedExamKind.GROUP)) }
                    )
                }
            }
            entry<GroupRoutineSubjectSelect> { route ->
                AdaptiveWidthContainer {
                    AdminGate(groupId = route.groupId, requireRoutineNotYetCreated = true, onDenied = { backStack.removeLastOrNull() }) {
                        GroupRoutineSubjectScreen(
                            groupId = route.groupId,
                            onBack = { backStack.removeLastOrNull() },
                            onNext = { subjectIds -> backStack.add(GroupRoutineChapterSelect(route.groupId, subjectIds)) }
                        )
                    }
                }
            }
            entry<GroupRoutineChapterSelect> { route ->
                AdaptiveWidthContainer {
                    AdminGate(groupId = route.groupId, onDenied = { backStack.removeLastOrNull() }) {
                        GroupRoutineChapterScreen(
                            groupId = route.groupId, subjectIds = route.subjectIds,
                            onBack = { backStack.removeLastOrNull() },
                            onNext = { chapterIds, topicIds ->
                                backStack.add(GroupRoutineWindowConfig(route.groupId, route.subjectIds, chapterIds, topicIds))
                            }
                        )
                    }
                }
            }
            entry<GroupRoutineWindowConfig> { route ->
                AdaptiveWidthContainer {
                    AdminGate(groupId = route.groupId, onDenied = { backStack.removeLastOrNull() }) {
                        GroupRoutineWindowScreen(
                            groupId = route.groupId, subjectIds = route.subjectIds, chapterIds = route.chapterIds, topicIds = route.topicIds,
                            onBack = { backStack.removeLastOrNull() },
                            onDone = {
                                // Pop the 3-step builder (Subject → Chapter → Window) back to GroupDetail.
                                repeat(3) { backStack.removeLastOrNull() }
                            }
                        )
                    }
                }
            }
            entry<GroupRoutineCalendar> { route ->
                AdaptiveWidthContainer {
                    GroupRoutineCalendarScreen(
                        groupId = route.groupId,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToGroupDetail = { backStack.removeLastOrNull() }
                    )
                }
            }
            entry<GroupPastExams> { route ->
                AdaptiveWidthContainer {
                    GroupPastExamsScreen(
                        groupId = route.groupId,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenExam = { examId -> backStack.add(WindowRanking(examId, TimedExamKind.GROUP)) }
                    )
                }
            }
            entry<GroupMembers> { route ->
                AdaptiveWidthContainer {
                    GroupMembersScreen(groupId = route.groupId, onBack = { backStack.removeLastOrNull() })
                }
            }

            // ── Live exam ──
            entry<LiveExamList> {
                AdaptiveWidthContainer {
                    LiveExamListScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToDetail = { examId -> backStack.add(LiveExamDetail(examId)) }
                    )
                }
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
