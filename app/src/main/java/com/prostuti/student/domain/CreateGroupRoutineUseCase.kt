package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CreateGroupRoutineRequest
import com.prostuti.student.data.repository.GroupExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateGroupRoutineUseCase(private val repository: GroupExamRepository) {
    suspend operator fun invoke(groupId: String, request: CreateGroupRoutineRequest): ApiResponse<Unit> =
        withContext(Dispatchers.IO) { repository.createRoutine(groupId, request) }
}
