package com.prostuti.student.domain.notification

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.FcmTokenData
import com.prostuti.student.data.repository.NotificationRepository

/** On logout: tells the server to stop pushing to this device. Must run before the session tokens are cleared. */
class ClearPushTokenUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(): ApiResponse<FcmTokenData> = repository.clearPushToken()
}
