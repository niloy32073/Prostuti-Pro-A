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
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
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
import com.prostuti.student.ui.component.IconBadge
import com.prostuti.student.ui.screen.auth.AuthLoginScreen
import com.prostuti.student.ui.screen.auth.AuthResetNewPasswordScreen
import com.prostuti.student.ui.screen.auth.AuthResetRequestScreen
import com.prostuti.student.ui.screen.auth.AuthSignupScreen
import com.prostuti.student.ui.screen.auth.AuthStartScreen
import com.prostuti.student.ui.screen.auth.AuthVerifyOtpScreen
import com.prostuti.student.ui.screen.intro.OnboardingScreen
import com.prostuti.student.ui.theme.ProstutiTypography

/**
 * The app's first level: Intro, the Auth sub-graph and Main (the tab shell, where every feature
 * screen lives — see [MainNavDisplay]).
 *
 * The Auth sub-graph (Auth → SignIn/SignUp → VerifyOtp/ResetRequest → ResetNewPassword) opts into
 * [ListDetailSceneStrategy]: on compact width the strategy declines and [NavDisplay] falls back to
 * its single-pane scene. On medium/expanded width Auth stays as the list pane with whichever step is
 * on top beside it. [calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth] gives two panes from
 * medium width (the library default only does from expanded), where most tablets in portrait land.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun RootNavDisplay(startDestination: RootRoute, innerPaddingValues: PaddingValues) {
    val backStack = rememberNavBackStack(startDestination)
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(windowAdaptiveInfo)
    }
    // System Back steps back one screen (like the on-screen arrow). The library default keeps popping while
    // the panes look the same, and PopUntilCurrentDestinationChange while the pane role stays "detail" —
    // both take members → group → list → out of the flow in one press.
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        backNavigationBehavior = BackNavigationBehavior.PopUntilContentChange,
        directive = directive,
    )
    val sceneStrategies = remember(listDetailStrategy) { listOf(listDetailStrategy) }
    // Pushes unless the same key is already on top: a double tap that lands before the screen's
    // lifecycle drops (dropUnlessResumed can't see it yet) never opens a screen twice.
    fun push(key: NavKey) {
        if (backStack.lastOrNull() != key) backStack.add(key)
    }
    // A step whose Back lands on Auth beside it hides its arrow (Auth is already on screen).
    // Deeper steps (OTP, reset…) keep theirs: Back takes them to the previous step.
    @Composable
    fun isRightBesideList(route: RootRoute, list: RootRoute): Boolean =
        isShownBesideList() && backStack.getOrNull(backStack.lastIndexOf(route) - 1) == list

    NavDisplay(
        modifier = Modifier.fillMaxSize().padding(innerPaddingValues),
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        sceneStrategies = sceneStrategies,
        // Never pop the last entry — it would hand NavDisplay an empty backstack and crash.
        // Intro and Main are each, at times, the sole entry; Auth guards itself with its own
        // BackHandler (exit-confirmation dialog) before a hardware back press gets here.
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Intro> {
                OnboardingScreen(onFinished = {
                    backStack.clear()
                    push(Auth)
                })
            }
            entry<Auth>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { AuthDetailPlaceholder() }
                )
            ) {
                AuthStartScreen(
                    onNavigateToLogin = { identifier, firstName ->
                        push(SignIn(identifier, firstName))
                    },
                    onNavigateToSignup = { identifier -> push(SignUp(identifier)) }
                )
            }
            entry<SignIn>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthLoginScreen(
                    identifier = route.identifier,
                    firstName = route.firstName,
                    onBack = { backStack.removeLastOrNull() },
                    onForgotPassword = { id -> push(ResetRequest(id)) },
                    onSignedIn = {
                        backStack.clear()
                        push(Main)
                    },
                    showBack = !isRightBesideList(route, Auth)
                )
            }
            entry<SignUp>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthSignupScreen(
                    identifier = route.identifier,
                    onBack = { backStack.removeLastOrNull() },
                    onSignedUp = { identifier -> push(VerifyOtp(identifier)) },
                    showBack = !isRightBesideList(route, Auth)
                )
            }
            entry<VerifyOtp>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthVerifyOtpScreen(
                    identifier = route.identifier,
                    onBack = { backStack.removeLastOrNull() },
                    onVerified = {
                        backStack.clear()
                        push(Main)
                    }
                )
            }
            entry<ResetRequest>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                AuthResetRequestScreen(
                    identifier = route.identifier,
                    onBack = { backStack.removeLastOrNull() },
                    onCodeEntered = { code -> push(ResetNewPassword(route.identifier, code)) }
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
                        push(Auth)
                    }
                )
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
