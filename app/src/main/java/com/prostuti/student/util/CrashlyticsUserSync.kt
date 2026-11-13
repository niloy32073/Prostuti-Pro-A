package com.prostuti.student.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.prostuti.student.data.service.local.CurrentUserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the Crashlytics user ID equal to the signed-in user's server ID (from the cached `auth/me`
 * row), and clears it on logout. Only the ID is sent — never name, phone or email.
 */
class CrashlyticsUserSync(
    private val crashlytics: FirebaseCrashlytics,
    private val currentUserDao: CurrentUserDao,
    private val appScope: CoroutineScope,
) {
    fun start() {
        appScope.launch {
            currentUserDao.observe()
                .map { it?.userId.orEmpty() }
                .distinctUntilChanged()
                .collect { crashlytics.setUserId(it) }
        }
    }
}
