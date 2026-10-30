package com.prostuti.student.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamResponseItem(
    @SerialName("mcq_id")
    val mcqId: String,
    @SerialName("selected_option_id")
    val selectedOptionId: String? = null,
    @SerialName("response_time")
    val responseTimeMs: Int
)

@Serializable
data class ExamResponseSubmitRequest(
    val responses: List<ExamResponseItem>
)
