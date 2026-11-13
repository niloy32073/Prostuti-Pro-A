package com.prostuti.student.data.service.remote

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirebasePushTokenSource : PushTokenSource {
    override suspend fun currentToken(): String? =
        try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "FCM token fetch failed")
            null
        }
}
