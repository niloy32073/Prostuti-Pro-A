package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.repository.GroupRepository

/** Groups I belong to, most recently joined first (limit 1-50). */
class FetchMyGroupsUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(page: Int = 1, limit: Int = 20): ApiResponse<List<StudyGroup>> = repository.myGroups(page, limit)
}
