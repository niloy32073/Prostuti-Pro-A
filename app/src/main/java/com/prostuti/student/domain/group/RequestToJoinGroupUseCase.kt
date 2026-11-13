package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupJoinRequest
import com.prostuti.student.data.repository.GroupRepository

/** Asks to join a public group; an admin must approve. */
class RequestToJoinGroupUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String): ApiResponse<GroupJoinRequest> = repository.requestToJoin(groupId)
}
