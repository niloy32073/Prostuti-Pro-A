package com.prostuti.student.domain.notification

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.NotificationRepository

/** Marks one notification read (idempotent). */
class MarkNotificationReadUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(notificationId: String): ApiResponse<Unit> = repository.markRead(notificationId)
}
