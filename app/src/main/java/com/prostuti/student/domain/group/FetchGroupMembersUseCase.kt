package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.data.repository.GroupRepository

/** A group's members, admins first (limit 1-100). */
class FetchGroupMembersUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String, page: Int = 1, limit: Int = 50): ApiResponse<List<StudyGroupMember>> = repository.members(groupId, page, limit)
}
