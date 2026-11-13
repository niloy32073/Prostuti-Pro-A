package com.prostuti.student.data.service.remote

import com.google.firebase.messaging.FirebaseMessagingService
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.NotificationRepository
import com.prostuti.student.data.repository.StorageRepository
import com.prostuti.student.di.APP_SCOPE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import timber.log.Timber

/**
 * FCM entry point. Firebase can rotate the token at any time; while someone is signed in the new
 * one is sent straight away so pushes keep reaching this device. Signed out → nothing is sent
 * (the next sign-in uploads the current token).
 */
class PushMessagingService : FirebaseMessagingService() {
    private val storageRepository: StorageRepository by inject()
    private val notificationRepository: NotificationRepository by inject()
    private val appScope: CoroutineScope by inject(named(APP_SCOPE))

    override fun onNewToken(token: String) {
        appScope.launch {
            if (storageRepository.getJWTToken().isNullOrBlank()) return@launch
            val push = notificationRepository.registerPushToken(token)
            if (push is ApiResponse.Failure) Timber.w("FCM token refresh upload failed: %s", push.errorCode)
        }
    }
}
