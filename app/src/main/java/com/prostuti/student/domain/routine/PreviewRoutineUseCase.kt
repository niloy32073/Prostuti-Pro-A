package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.request.RoutineCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutinePlan
import com.prostuti.student.data.repository.RoutineRepository

/** Computes a routine's schedule and planner warnings without saving anything. */
class PreviewRoutineUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(body: RoutineCreateRequest): ApiResponse<RoutinePlan> = repository.preview(body)
}
