package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.repository.GroupRepository

/** One group as I see it (invite code only if I'm an admin). */
class FetchGroupUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String): ApiResponse<StudyGroup> = repository.group(groupId)
}
