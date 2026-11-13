package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FcmTokenData
import com.prostuti.student.data.model.response.NotificationItem
import com.prostuti.student.data.repository.NotificationRepository

/** Records push-token calls; the inbox methods return empty results. */
class FakeNotificationRepository(
    /** Runs inside [clearPushToken], e.g. to check the session token still exists at that moment. */
    private val onClear: suspend () -> Unit = {},
) : NotificationRepository {
    val registeredTokens = mutableListOf<String?>()
    var clearCount = 0

    override suspend fun list(unreadOnly: Boolean, page: Int, limit: Int): ApiResponse<List<NotificationItem>> =
        ApiResponse.Success(emptyList())

    override suspend fun markRead(notificationId: String): ApiResponse<Unit> = ApiResponse.Success(Unit)

    override suspend fun registerPushToken(token: String?): ApiResponse<FcmTokenData> {
        registeredTokens += token
        return ApiResponse.Success(FcmTokenData(registered = true))
    }

    override suspend fun clearPushToken(): ApiResponse<FcmTokenData> {
        onClear()
        clearCount++
        return ApiResponse.Success(FcmTokenData(registered = false))
    }
}
