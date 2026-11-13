package com.prostuti.student.navigation

import androidx.navigation3.runtime.NavKey

/** The only place that changes the Main shell's back stacks. */
class Navigator(val state: NavigationState) {

    /**
     * A tab: show it, with the history it had. Anything else: push it onto the shown tab, unless it
     * is already on top (a double tap that lands before dropUnlessResumed can see it).
     */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) state.topLevelRoute = route
        else if (state.currentStack.last() != route) state.currentStack.add(route)
    }

    /** Pops the shown tab; from another tab's root, goes back to the start tab. */
    fun goBack() {
        val stack = state.currentStack
        if (stack.last() == state.topLevelRoute) state.topLevelRoute = state.startRoute
        else stack.removeLastOrNull()
    }
}
