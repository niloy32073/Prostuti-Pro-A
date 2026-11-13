package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FcmTokenData
import com.prostuti.student.data.model.response.NotificationItem

/** The in-app notification inbox and this device's push registration. */
interface NotificationRepository {
    suspend fun list(unreadOnly: Boolean, page: Int, limit: Int): ApiResponse<List<NotificationItem>>
    suspend fun markRead(notificationId: String): ApiResponse<Unit>

    /** Sends this device's FCM token for the signed-in user. [token] null = ask Firebase for the current one. */
    suspend fun registerPushToken(token: String? = null): ApiResponse<FcmTokenData>

    /** Sends `token: null` so the server stops pushing to this device. Call while still signed in. */
    suspend fun clearPushToken(): ApiResponse<FcmTokenData>
}
