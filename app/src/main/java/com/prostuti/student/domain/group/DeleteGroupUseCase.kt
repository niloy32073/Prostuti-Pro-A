package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupActionAck
import com.prostuti.student.data.repository.GroupRepository

/** Deletes a group (admin, soft delete; members keep their history). */
class DeleteGroupUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String): ApiResponse<GroupActionAck> = repository.deleteGroup(groupId)
}
