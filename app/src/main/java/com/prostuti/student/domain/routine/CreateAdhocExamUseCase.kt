package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.request.AdhocExamRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.repository.RoutineRepository

/** Schedules a one-off group exam (admin). */
class CreateAdhocExamUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(body: AdhocExamRequest): ApiResponse<RoutineSlot> = repository.createAdhoc(body)
}
