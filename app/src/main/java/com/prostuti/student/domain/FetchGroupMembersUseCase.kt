package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupMember
import com.prostuti.student.data.repository.GroupExamRepository

class FetchGroupMembersUseCase(private val repository: GroupExamRepository) {
    suspend operator fun invoke(groupId: String): ApiResponse<List<GroupMember>> =
        repository.members(groupId)
}
