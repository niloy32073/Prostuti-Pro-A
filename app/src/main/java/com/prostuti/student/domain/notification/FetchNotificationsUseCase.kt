package com.prostuti.student.domain.notification

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.NotificationItem
import com.prostuti.student.data.repository.NotificationRepository

/** My notifications, newest first (limit 1-50). */
class FetchNotificationsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(unreadOnly: Boolean = false, page: Int = 1, limit: Int = 20): ApiResponse<List<NotificationItem>> = repository.list(unreadOnly, page, limit)
}
