package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.request.FcmTokenRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FcmTokenData
import com.prostuti.student.data.model.response.NotificationItem
import com.prostuti.student.util.safeApiCall
import com.prostuti.student.util.safeApiCallNoData
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** In-app notification inbox and push device registration. */
class NotificationApiService(private val client: HttpClient) {

    suspend fun list(unreadOnly: Boolean, page: Int, limit: Int): ApiResponse<List<NotificationItem>> = safeApiCall {
        client.get("notifications") {
            if (unreadOnly) parameter("unread_only", true)
            paging(page, limit)
        }
    }

    /** Idempotent. */
    suspend fun markRead(notificationId: String): ApiResponse<Unit> = safeApiCallNoData {
        client.post("notifications/$notificationId/read")
    }

    /** One device per user: replaces the previous token. `null` stops pushes (logout). */
    suspend fun registerFcmToken(token: String?): ApiResponse<FcmTokenData> = safeApiCall {
        client.put("notifications/fcm-token") {
            contentType(ContentType.Application.Json)
            setBody(FcmTokenRequest(token))
        }
    }
}
