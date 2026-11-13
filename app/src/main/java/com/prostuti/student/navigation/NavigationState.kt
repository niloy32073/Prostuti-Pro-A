package com.prostuti.student.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer

/**
 * The Main shell's tabs: one back stack per tab, plus which tab is shown. Both are saved, so the
 * tabs' histories survive rotation and process death.
 */
@Composable
fun rememberNavigationState(startRoute: NavKey, topLevelRoutes: Set<NavKey>): NavigationState {
    val topLevelRoute: MutableState<NavKey> = rememberSerializable(startRoute, topLevelRoutes, stateSerializer = NavKeySerializer()) {
        mutableStateOf(startRoute)
    }
    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }
    return remember(startRoute, topLevelRoutes) { NavigationState(startRoute, topLevelRoute, backStacks) }
}

class NavigationState(
    /** The tab the app opens on and leaves from. */
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    /** Each tab's history; its first key is the tab itself. */
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    /** The tab shown now. */
    var topLevelRoute: NavKey by topLevelRoute
    val currentStack: NavBackStack<NavKey> get() = backStacks.getValue(topLevelRoute)

    /** The start tab sits under every other tab, so Back from a tab's root lands on it. */
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) listOf(startRoute) else listOf(startRoute, topLevelRoute)
}

/**
 * Every tab's entries are decorated (so a hidden tab keeps its ViewModels and saved UI state), but
 * only the start tab and the shown tab go to NavDisplay.
 */
@Composable
fun NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): SnapshotStateList<NavEntry<NavKey>> {
    val decorated = backStacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>(),
            ),
            entryProvider = entryProvider,
        )
    }
    return stacksInUse.flatMap { decorated[it].orEmpty() }.toMutableStateList()
}
