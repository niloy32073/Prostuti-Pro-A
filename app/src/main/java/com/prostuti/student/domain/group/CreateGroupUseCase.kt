package com.prostuti.student.domain.group

import com.prostuti.student.data.model.request.GroupCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.repository.GroupRepository

/** Creates a group; the caller becomes its first admin and gets the invite code. */
class CreateGroupUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(body: GroupCreateRequest): ApiResponse<StudyGroup> = repository.createGroup(body)
}
