package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.repository.RoutineRepository
import java.time.LocalDate

/** My schedule between two Dhaka days (inclusive), optionally one group's only (limit 1-100). */
class FetchSlotsUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(dateFrom: LocalDate? = null, dateTo: LocalDate? = null, groupId: String? = null, page: Int = 1, limit: Int = 100): ApiResponse<List<RoutineSlot>> = repository.slots(dateFrom?.toString(), dateTo?.toString(), groupId, page, limit)
}
