package com.prostuti.student.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import com.prostuti.student.ui.screen.home.HomeScreen
import com.prostuti.student.ui.screen.settings.SettingsScreen
import com.prostuti.student.navigation.QBNavDisplay

private data class MainTab(val route: MainRoute, val labelRes: Int, val icon: ImageVector)

private val mainTabs = listOf(
    MainTab(Home, R.string.nav_home, Icons.Filled.Home),
    MainTab(QB, R.string.nav_question_bank, Icons.AutoMirrored.Filled.MenuBook),
    MainTab(Leaderboard, R.string.nav_leaderboard, Icons.Filled.Leaderboard),
    MainTab(Routine, R.string.nav_routine, Icons.Filled.CalendarMonth),
    MainTab(Settings, R.string.nav_settings, Icons.Filled.Settings)
)

/**
 * The post-login shell: a bottom [NavigationSuiteType.NavigationBar] on compact width, a
 * [NavigationSuiteType.NavigationRail] on medium AND expanded (per the architecture skill's
 * adaptive-navigation-suite guidance — the default NavigationSuiteScaffold would otherwise
 * switch to a permanent drawer at expanded width, which isn't what was asked for here).
 * Only Home has real content right now; QB/Leaderboard/Routine are stand-in screens until their
 * own screen-by-screen work lands.
 */
@Composable
fun MainNavDisplay(
    onLoggedOut: () -> Unit,
    onNavigateToExamEngine: (ExamAttempt) -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToGroupList: () -> Unit,
    onNavigateToLiveExamList: () -> Unit,
    onNavigateToGroupDetail: (groupId: String) -> Unit
) {
    val backStack = rememberNavBackStack(Home)
    val qbBackStack = rememberNavBackStack(SubjectList)
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val current = backStack.lastOrNull() as? MainRoute ?: Home
    val isInsideQB = current == QB && qbBackStack.size > 1
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
                        onNavigateToGroupDetail = onNavigateToGroupDetail
                    )
                }
                entry<QB> { QBNavDisplay(qbBackStack, onNavigateToExamEngine = onNavigateToExamEngine) }
                entry<Leaderboard> { ComingSoonScreen(title = stringResource(R.string.nav_leaderboard)) }
                entry<Routine> { ComingSoonScreen(title = stringResource(R.string.nav_routine)) }
                entry<Settings> { SettingsScreen(onLoggedOut = onLoggedOut) }
            }
        )
    }
}
