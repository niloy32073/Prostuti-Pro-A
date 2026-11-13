package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.repository.GroupRepository

/** Replaces the join code (admin); the old code stops working at once. */
class RegenerateGroupCodeUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String): ApiResponse<StudyGroup> = repository.regenerateCode(groupId)
}
