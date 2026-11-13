package com.prostuti.student.domain.group

import com.prostuti.student.data.model.enums.JoinRequestStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupJoinRequest
import com.prostuti.student.data.repository.GroupRepository

/** A group's join requests, oldest first (admin). */
class FetchJoinRequestsUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String, status: JoinRequestStatus = JoinRequestStatus.PENDING, page: Int = 1, limit: Int = 20): ApiResponse<List<GroupJoinRequest>> = repository.joinRequests(groupId, status, page, limit)
}
