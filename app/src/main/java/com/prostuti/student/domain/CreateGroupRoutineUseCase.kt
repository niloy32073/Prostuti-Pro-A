package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CreateGroupRoutineRequest
import com.prostuti.student.data.repository.GroupExamRepository

class CreateGroupRoutineUseCase(private val repository: GroupExamRepository) {
    suspend operator fun invoke(groupId: String, request: CreateGroupRoutineRequest): ApiResponse<Unit> =
        repository.createRoutine(groupId, request)
}
