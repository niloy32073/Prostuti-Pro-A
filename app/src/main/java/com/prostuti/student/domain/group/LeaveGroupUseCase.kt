package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupActionAck
import com.prostuti.student.data.repository.GroupRepository

/** Leaves a group. Fails with LAST_ADMIN for the only admin. */
class LeaveGroupUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String): ApiResponse<GroupActionAck> = repository.leaveGroup(groupId)
}
