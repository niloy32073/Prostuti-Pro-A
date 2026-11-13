package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.RoutineRepository

/** Removes a one-off group exam nobody has attempted (admin). */
class DeleteAdhocExamUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(slotId: String): ApiResponse<Unit> = repository.deleteAdhoc(slotId)
}
