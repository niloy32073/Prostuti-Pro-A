package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamResponseAckResponse(
    @SerialName("mcq_id")
    val mcqId: String,
    @SerialName("selected_option_id")
    val selectedOptionId: String? = null,
    @SerialName("response_time")
    val responseTimeMs: Int
)
