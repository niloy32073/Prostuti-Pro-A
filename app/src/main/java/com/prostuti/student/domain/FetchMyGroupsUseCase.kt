package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Group
import com.prostuti.student.data.repository.GroupExamRepository
import kotlinx.coroutines.flow.Flow

class FetchMyGroupsUseCase(private val repository: GroupExamRepository) {
    operator fun invoke(): Flow<ApiResponse<List<Group>>> = repository.myGroups()
}
