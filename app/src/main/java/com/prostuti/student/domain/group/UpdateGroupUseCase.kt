package com.prostuti.student.domain.group

import com.prostuti.student.data.model.request.GroupUpdateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.repository.GroupRepository

/** Edits a group (admin). Only the non-null fields change. */
class UpdateGroupUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String, body: GroupUpdateRequest): ApiResponse<StudyGroup> = repository.updateGroup(groupId, body)
}
