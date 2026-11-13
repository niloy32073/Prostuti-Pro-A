package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupJoinRequest
import com.prostuti.student.data.repository.GroupRepository

/** Rejects a pending request (admin); the user may ask again later. */
class RejectJoinRequestUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String, requestId: String): ApiResponse<GroupJoinRequest> = repository.rejectJoinRequest(groupId, requestId)
}
