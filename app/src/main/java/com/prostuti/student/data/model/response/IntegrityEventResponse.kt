package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.IntegrityEventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntegrityEventResponse(
    @SerialName("id")
    val id: String,
    @SerialName("attempt_id")
    val attemptId: String,
    @SerialName("event_type")
    val eventType: IntegrityEventType,
    @SerialName("occurred_at")
    val occurredAt: String,
    @SerialName("event_context")
    val eventContext: String? = null
)
