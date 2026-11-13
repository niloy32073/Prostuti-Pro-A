package com.prostuti.student.navigation

import org.koin.compose.koinInject
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.prostuti.student.R
import com.prostuti.student.ui.screen.ComingSoonScreen
import com.prostuti.student.ui.component.NotificationPermissionRequest
import com.prostuti.student.ui.screen.home.HomeScreen
import com.prostuti.student.ui.screen.settings.SettingsScreen
import com.prostuti.student.navigation.QBNavDisplay

private data class MainTab(val route: MainRoute, val labelRes: Int, val icon: ImageVector)

private val mainTabs = listOf(
    MainTab(Home, R.string.nav_home, Icons.Filled.Home),
    MainTab(QB, R.string.nav_question_bank, Icons.AutoMirrored.Filled.MenuBook),
    MainTab(Leaderboard, R.string.nav_leaderboard, Icons.Filled.Leaderboard),
    MainTab(Routine, R.string.nav_routine, Icons.Filled.CalendarMonth),
    MainTab(Settings, R.string.nav_account, Icons.Filled.AccountCircle)
)

/**
 * The post-login shell: a bottom [NavigationSuiteType.NavigationBar] on compact width, a
 * [NavigationSuiteType.NavigationRail] on medium AND expanded (per the architecture skill's
 * adaptive-navigation-suite guidance — the default NavigationSuiteScaffold would otherwise
 * switch to a permanent drawer at expanded width, which isn't what was asked for here).
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
    val backStack = rememberNavBackStack(Home)
    val qbBackStack = rememberNavBackStack(SubjectList)
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val current = backStack.lastOrNull() as? MainRoute ?: Home
    // A full-screen page (e.g. Notifications) can ask for a tab when it closes.
    val tabRequests: MainTabRequests = koinInject()
    val requestedTab by tabRequests.pending.collectAsStateWithLifecycle()
    LaunchedEffect(requestedTab) {
        requestedTab?.let { tab ->
            tabRequests.consume()
            if (backStack.lastOrNull() != tab) {
                backStack.clear()
                backStack.add(tab)
            }
        }
    }
    val isInsideQB = current == QB && qbBackStack.size > 1
    // Back on a tab root would close the app — ask first. Inside a QB sub-screen, back pops it instead.
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val activity = LocalActivity.current
    BackHandler(enabled = !isInsideQB) { showExitDialog = true }
    if (showExitDialog) {
        ExitAppDialog(
            onExit = { showExitDialog = false; activity?.finish() },
            onDismiss = { showExitDialog = false }
        )
    }
    val layoutType = when {
        isInsideQB -> NavigationSuiteType.None
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> NavigationSuiteType.NavigationRail
        else -> NavigationSuiteType.NavigationBar
    }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            mainTabs.forEach { tab ->
                item(
                    selected = current == tab.route,
                    onClick = {
                        if (backStack.lastOrNull() != tab.route) {
                            backStack.clear()
                            backStack.add(tab.route)
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(stringResource(tab.labelRes)) }
                )
            }
        }
    ) {
        NavDisplay(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            onBack = { /* a single-entry tab backstack — nothing to pop */ },
            entryProvider = entryProvider {
                entry<Home> {
                    HomeScreen(
                        onNavigateToQB = {
                            if (backStack.lastOrNull() != QB) {
                                backStack.clear()
                                backStack.add(QB)
                            }
                        },
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
                                backStack.clear()
                                backStack.add(Settings)
                            } else {
                                onNavigateToProfile()
                            }
                        },
                        onNavigateToPlayground = onNavigateToPlayground,
                        onNavigateToNotifications = { onOpenRoute(Notifications) },
                        onNavigateToRoutine = {
                            if (backStack.lastOrNull() != Routine) {
                                backStack.clear()
                                backStack.add(Routine)
                            }
                        }
                    )
                }
                entry<QB> { QBNavDisplay(qbBackStack, onNavigateToExamEngine = onNavigateToExamEngine) }
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
        )
    }
}
