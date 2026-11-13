package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Result of deleting a routine: how many upcoming, unattempted exams were removed. */
@Serializable
data class RoutineDeleteResult(
    @SerialName("routine_id") val routineId: String,
    @SerialName("removed_slots") val removedSlots: Int,
)
