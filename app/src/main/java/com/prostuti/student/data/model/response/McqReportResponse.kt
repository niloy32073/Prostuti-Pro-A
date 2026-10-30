package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class McqReportResponse(
    val id: String,
    @SerialName("mcq_id")
    val mcqId: String,
    @SerialName("reported_by")
    val reportedBy: String
)
