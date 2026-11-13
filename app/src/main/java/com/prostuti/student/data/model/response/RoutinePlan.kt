package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Result of `POST /routines/preview`. [warnings] must always be shown to the user. */
@Serializable
data class RoutinePlan(
    @SerialName("exam_days") val examDays: Int,
    @SerialName("slots") val slots: List<PlannedSlot> = emptyList(),
    @SerialName("warnings") val warnings: List<String> = emptyList(),
)
