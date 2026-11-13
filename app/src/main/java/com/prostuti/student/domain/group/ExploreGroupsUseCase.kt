package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.repository.GroupRepository

/** Public groups, optionally searched by name/description (limit 1-50). */
class ExploreGroupsUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(query: String? = null, page: Int = 1, limit: Int = 20): ApiResponse<List<StudyGroup>> = repository.exploreGroups(query, page, limit)
}
