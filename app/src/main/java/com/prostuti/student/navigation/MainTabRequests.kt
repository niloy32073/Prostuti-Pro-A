package com.prostuti.student.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lets a full-screen destination (e.g. Notifications) ask the bottom-nav host to show a tab once it
 * is back on top. App-wide `single`; [MainNavDisplay] consumes the request.
 */
class MainTabRequests {
    private val _pending = MutableStateFlow<MainRoute?>(null)
    val pending: StateFlow<MainRoute?> = _pending.asStateFlow()

    fun request(tab: MainRoute) {
        _pending.value = tab
    }

    fun consume() {
        _pending.value = null
    }
}
