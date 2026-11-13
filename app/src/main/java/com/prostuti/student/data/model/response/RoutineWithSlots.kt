package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** A routine plus its day-wise exams and my status on each. [warnings] is only filled on creation. */
@Serializable
data class RoutineWithSlots(
    @SerialName("routine") val routine: ExamRoutine,
    @SerialName("slots") val slots: List<RoutineSlot> = emptyList(),
    @SerialName("warnings") val warnings: List<String> = emptyList(),
)
