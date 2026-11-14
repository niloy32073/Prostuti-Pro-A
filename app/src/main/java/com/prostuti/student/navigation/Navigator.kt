package com.prostuti.student.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** The only place that changes the Main shell's back stacks. */
class Navigator(val state: NavigationState) {

    private val stack: NavBackStack<NavKey> get() = state.currentStack

    /**
     * A tab: show it, with the history it had. Anything else: push it onto the shown tab, unless it
     * is already on top (a double tap that lands before dropUnlessResumed can see it).
     */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) state.topLevelRoute = route
        else if (stack.last() != route) stack.add(route)
    }

    /** Pops the shown tab; from another tab's root, goes back to the start tab. */
    fun goBack() {
        if (stack.last() == state.topLevelRoute) state.topLevelRoute = state.startRoute
        else stack.removeLastOrNull()
    }

    /** Swaps the top screen for [route] (an exam that was just submitted → its result). */
    fun replaceTop(route: NavKey) {
        if (stack.size > 1) stack.removeLastOrNull()
        navigate(route)
    }

    /** Pops the shown tab down to its root. */
    fun popToRoot() {
        while (stack.size > 1) stack.removeLastOrNull()
    }

    /** Pops the shown tab until [isTarget] is on top, never past the tab's root. */
    fun popUntil(isTarget: (NavKey) -> Boolean) {
        while (stack.size > 1 && !isTarget(stack.last())) stack.removeLastOrNull()
    }

    /** Removes matching screens from every tab (a group that is gone), never a tab's root. */
    fun removeEverywhere(matches: (NavKey) -> Boolean) {
        state.backStacks.values.forEach { tabStack -> tabStack.removeAll { it !in state.backStacks.keys && matches(it) } }
    }

    /** Shows [tab] with [screens] on top of its root, replacing that tab's history (a link from outside the app). */
    fun openInTab(tab: NavKey, screens: List<NavKey>) {
        state.topLevelRoute = tab
        val tabStack = state.backStacks.getValue(tab)
        while (tabStack.size > 1) tabStack.removeLastOrNull()
        tabStack.addAll(screens)
    }

    /**
     * The tab stack holding [route] (the shown one if none does). A screen asks about its own stack
     * (what is beside it, what is open on top of it): during a tab switch the old tab is still drawn.
     */
    fun stackOf(route: NavKey): List<NavKey> = state.backStacks.values.firstOrNull { route in it } ?: stack

    /** True when any tab holds a screen matching [matches]. */
    fun anyOpen(matches: (NavKey) -> Boolean): Boolean = state.backStacks.values.any { tabStack -> tabStack.any(matches) }
}
