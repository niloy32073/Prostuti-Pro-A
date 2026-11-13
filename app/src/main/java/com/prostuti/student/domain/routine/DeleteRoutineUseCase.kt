package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineDeleteResult
import com.prostuti.student.data.repository.RoutineRepository

/** Deletes a routine; unattempted upcoming exams go, history stays. */
class DeleteRoutineUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(routineId: String): ApiResponse<RoutineDeleteResult> = repository.delete(routineId)
}
