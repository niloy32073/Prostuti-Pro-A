package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Result of starting a slot: the shared exam to attempt, and whether the attempt will count as LIVE. */
@Serializable
data class SlotStart(
    @SerialName("slot_id") val slotId: String,
    @SerialName("exam_id") val examId: String,
    @SerialName("is_live") val isLive: Boolean,
    @SerialName("closes_at") val closesAt: String,
)
