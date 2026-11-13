package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.SlotStart
import com.prostuti.student.data.repository.RoutineRepository

/** Gets (creating on first use) the shared exam behind a slot. */
class StartSlotUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(slotId: String): ApiResponse<SlotStart> = repository.start(slotId)
}
