package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupActionAck
import com.prostuti.student.data.repository.GroupRepository

/** Removes a member or another admin (admin). */
class RemoveGroupMemberUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String, userId: String): ApiResponse<GroupActionAck> = repository.removeMember(groupId, userId)
}
