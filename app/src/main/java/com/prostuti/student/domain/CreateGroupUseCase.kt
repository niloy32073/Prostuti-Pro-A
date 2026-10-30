package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Group
import com.prostuti.student.data.repository.GroupExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateGroupUseCase(private val repository: GroupExamRepository) {
    suspend operator fun invoke(nameBn: String): ApiResponse<Group> =
        withContext(Dispatchers.IO) { repository.createGroup(nameBn) }
}
