package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.request.RoutineCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.data.repository.RoutineRepository

/** Saves a routine and its exam days. Fails with ROUTINE_EXISTS when one is already active. */
class CreateRoutineUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(body: RoutineCreateRequest): ApiResponse<RoutineWithSlots> = repository.create(body)
}
