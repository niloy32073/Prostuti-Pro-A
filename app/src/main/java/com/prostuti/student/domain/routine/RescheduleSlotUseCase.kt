package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.repository.RoutineRepository
import java.time.LocalDate

/** Moves a slot to another Dhaka day before its window opens; the window times stay. */
class RescheduleSlotUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(slotId: String, slotDate: LocalDate): ApiResponse<RoutineSlot> = repository.reschedule(slotId, slotDate.toString())
}
