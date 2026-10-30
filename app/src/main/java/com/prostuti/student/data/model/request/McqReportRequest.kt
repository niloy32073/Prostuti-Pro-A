package com.prostuti.student.data.model.request

import kotlinx.serialization.Serializable

@Serializable
data class McqReportRequest(
    val reason: String,
    val description: String? = null
)
