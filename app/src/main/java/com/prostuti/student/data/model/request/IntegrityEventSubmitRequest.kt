package com.prostuti.student.data.model.request

import com.prostuti.student.data.model.enums.IntegrityEventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IntegrityEventItem(
    @SerialName("event_type")
    val eventType: IntegrityEventType,
    @SerialName("event_context")
    val eventContext: String? = null
)

@Serializable
data class IntegrityEventSubmitRequest(
    val events: List<IntegrityEventItem>
)
