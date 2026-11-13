package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupJoinRequest
import com.prostuti.student.data.repository.GroupRepository

/** Approves a pending request (admin); the user becomes a member. */
class ApproveJoinRequestUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String, requestId: String): ApiResponse<GroupJoinRequest> = repository.approveJoinRequest(groupId, requestId)
}
