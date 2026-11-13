package com.prostuti.student.navigation

import org.koin.compose.koinInject
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.prostuti.student.ui.component.ExitAppDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostuti.student.ui.screen.routine.RoutineTabScreen
import com.prostuti.student.ui.screen.routine.builder.RoutineOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.ui.screen.ComingSoonScreen
import com.prostuti.student.ui.component.NotificationPermissionRequest
import com.prostuti.student.ui.screen.home.HomeScreen
import com.prostuti.student.ui.screen.qb.ChapterBrowseScreen
import com.prostuti.student.ui.screen.qb.MCQExamListScreen
import com.prostuti.student.ui.screen.qb.QuestionReaderScreen
import com.prostuti.student.ui.screen.qb.SubjectListScreen
import com.prostuti.student.ui.screen.qb.SubjectTypeScreen
import com.prostuti.student.ui.screen.settings.SettingsScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private data class MainTab(val route: MainRoute, val labelRes: Int, val icon: ImageVector)

private val mainTabs = listOf(
    MainTab(Home, R.string.nav_home, Icons.Filled.Home),
    MainTab(QB, R.string.nav_question_bank, Icons.AutoMirrored.Filled.MenuBook),
    MainTab(Leaderboard, R.string.nav_leaderboard, Icons.Filled.Leaderboard),
    MainTab(Routine, R.string.nav_routine, Icons.Filled.CalendarMonth),
    MainTab(Settings, R.string.nav_account, Icons.Filled.AccountCircle)
)

private val mainTabRoutes: Set<NavKey> = mainTabs.map { it.route }.toSet()

/**
 * The post-login shell: a bottom [NavigationSuiteType.NavigationBar] on compact width, a
 * [NavigationSuiteType.NavigationRail] on medium AND expanded (per the architecture skill's
 * adaptive-navigation-suite guidance — the default NavigationSuiteScaffold would otherwise
 * switch to a permanent drawer at expanded width, which isn't what was asked for here).
 * Each tab keeps its own history ([NavigationState]); the Question Bank's screens are pushed onto
 * its tab. Back from a tab's root goes to Home; on Home it asks before closing the app.
 * Leaderboard is still a stand-in screen until its own work lands.
 */
@Composable
fun MainNavDisplay(
    onLoggedOut: () -> Unit,
    onNavigateToExamEngine: (ExamAttempt) -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToLiveExamList: () -> Unit,
    onNavigateToGroupDetail: (groupId: String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPlayground: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToExamHistory: () -> Unit,
    /** Opens a full-screen destination from the Routine tab (builder, exam, result, ranking). */
    onOpenRoute: (RootRoute) -> Unit
) {
    // Signed in → push token is on the server; ask (Android 13+) so pushes can actually be shown.
    NotificationPermissionRequest()
    val navigationState = rememberNavigationState(startRoute = Home, topLevelRoutes = mainTabRoutes)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    // A full-screen page (e.g. Notifications) can ask for a tab when it closes.
    val tabRequests: MainTabRequests = koinInject()
    val requestedTab by tabRequests.pending.collectAsStateWithLifecycle()
    LaunchedEffect(requestedTab) {
        requestedTab?.let { tab ->
            tabRequests.consume()
            navigator.navigate(tab)
        }
    }
    val isInsideQB = navigationState.topLevelRoute == QB && navigationState.currentStack.size > 1
    // Back on Home's root would close the app — ask first. Every other Back is NavDisplay's (navigator.goBack).
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val activity = LocalActivity.current
    BackHandler(enabled = navigationState.topLevelRoute == Home && navigationState.currentStack.size == 1) {
        showExitDialog = true
    }
    if (showExitDialog) {
        ExitAppDialog(
            onExit = { showExitDialog = false; activity?.finish() },
            onDismiss = { showExitDialog = false }
        )
    }
    val layoutType = if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }
    // Inside the Question Bank the bar/rail slides away (state, not layoutType, so it animates).
    val scaffoldState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(isInsideQB) { if (isInsideQB) scaffoldState.hide() else scaffoldState.show() }

    val entryProvider = entryProvider {
        entry<Home> {
            HomeScreen(
                onNavigateToQB = { navigator.navigate(QB) },
                onNavigateToPractice = onNavigateToPractice,
                onNavigateToGroupList = onNavigateToGroupList,
                onNavigateToLiveExamList = onNavigateToLiveExamList,
                onNavigateToGroupDetail = onNavigateToGroupDetail,
                onContinueAttempt = { attemptId, titleBn ->
                    onNavigateToExamEngine(ExamAttempt(attemptId = attemptId, titleBn = titleBn))
                },
                // Medium and wider: the Account tab already shows the profile beside the settings.
                onNavigateToProfile = {
                    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
                        navigator.navigate(Settings)
                    } else {
                        onNavigateToProfile()
                    }
                },
                onNavigateToPlayground = onNavigateToPlayground,
                onNavigateToNotifications = { onOpenRoute(Notifications) },
                onNavigateToRoutine = { navigator.navigate(Routine) }
            )
        }
        // ── Question Bank ── the tab's root is the subject list; its screens stack on the QB tab.
        entry<QB> { SubjectListScreen(onNavigate = navigator::navigate) }
        entry<SubjectType> { route ->
            SubjectTypeScreen(
                subjectId = route.subjectId,
                nameBn = route.nameBn,
                nameEn = route.nameEn,
                onBack = navigator::goBack,
                onNavigateToMCQ = navigator::navigate
            )
        }
        entry<MCQExamList> { route ->
            MCQExamListScreen(
                subjectId = route.subjectId,
                nameBn = route.nameBn,
                nameEn = route.nameEn,
                onBack = navigator::goBack,
                onNavigateToExamEngine = onNavigateToExamEngine,
                onNavigateToReader = navigator::navigate,
                onNavigateToChapterBrowse = navigator::navigate
            )
        }
        entry<QuestionReader> { route ->
            QuestionReaderScreen(
                titleBn = route.titleBn,
                onBack = navigator::goBack,
                viewModel = koinViewModel { parametersOf(route.examId) }
            )
        }
        entry<ChapterBrowse> { route ->
            ChapterBrowseScreen(
                subjectId = route.subjectId,
                nameBn = route.nameBn,
                nameEn = route.nameEn,
                onBack = navigator::goBack
            )
        }
        entry<Leaderboard> { ComingSoonScreen(title = stringResource(R.string.nav_leaderboard)) }
        entry<Routine> {
            RoutineTabScreen(
                onCreatePersonalRoutine = { onOpenRoute(RoutineBuilder(RoutineOwner.Personal)) },
                onOpenGroups = onNavigateToGroupList,
                onOpenExamAttempt = { id, title, lock -> onOpenRoute(ExamAttempt(id, title, lock)) },
                onOpenExamResult = { id, title, lock -> onOpenRoute(ExamResult(id, title, fromHistory = true, lockResultUntil = lock)) },
                onOpenRanking = { groupId, examId, title, closes -> onOpenRoute(GroupRanking(groupId, examId, title, closes)) },
            )
        }
        entry<Settings> {
            SettingsScreen(
                onLoggedOut = onLoggedOut,
                onOpenBookmarks = onNavigateToBookmarks,
                onOpenExamHistory = onNavigateToExamHistory,
                onOpenProfile = onNavigateToProfile,
                onOpenPlayground = onNavigateToPlayground
            )
        }
    }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        state = scaffoldState,
        navigationSuiteItems = {
            mainTabs.forEach { tab ->
                item(
                    selected = navigationState.topLevelRoute == tab.route,
                    onClick = { navigator.navigate(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.labelRes)) }
                )
            }
        }
    ) {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = navigator::goBack,
        )
    }
}
