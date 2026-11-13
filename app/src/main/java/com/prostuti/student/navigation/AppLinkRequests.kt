package com.prostuti.student.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A link the app was opened with (widget tap), waiting for [RootNavDisplay] to open it. App-wide
 * `single`: [com.prostuti.student.MainActivity] posts it, the navigation host consumes it once.
 */
class AppLinkRequests {
    private val _pending = MutableStateFlow<AppLink?>(null)
    val pending: StateFlow<AppLink?> = _pending.asStateFlow()

    fun request(link: AppLink) {
        _pending.value = link
    }

    fun consume() {
        _pending.value = null
    }
}
