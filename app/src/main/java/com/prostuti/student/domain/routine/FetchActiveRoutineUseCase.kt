package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.data.repository.RoutineRepository

/** The active routine of a group, or my personal one when [groupId] is null; null data = none. */
class FetchActiveRoutineUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(groupId: String? = null): ApiResponse<RoutineWithSlots?> = repository.active(groupId)
}
