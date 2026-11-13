package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FcmTokenData
import com.prostuti.student.data.service.remote.NotificationApiService
import com.prostuti.student.data.service.remote.PushTokenSource

class NotificationRepositoryImpl(
    private val api: NotificationApiService,
    private val pushTokenSource: PushTokenSource,
) : NotificationRepository {
    override suspend fun list(unreadOnly: Boolean, page: Int, limit: Int) = api.list(unreadOnly, page, limit)
    override suspend fun markRead(notificationId: String) = api.markRead(notificationId)

    override suspend fun registerPushToken(token: String?): ApiResponse<FcmTokenData> {
        val deviceToken = token ?: pushTokenSource.currentToken()
            ?: return ApiResponse.Failure("Push token unavailable", PUSH_TOKEN_UNAVAILABLE)
        return api.registerFcmToken(deviceToken)
    }

    override suspend fun clearPushToken(): ApiResponse<FcmTokenData> = api.registerFcmToken(null)

    companion object {
        const val PUSH_TOKEN_UNAVAILABLE = "PUSH_TOKEN_UNAVAILABLE"
    }
}
