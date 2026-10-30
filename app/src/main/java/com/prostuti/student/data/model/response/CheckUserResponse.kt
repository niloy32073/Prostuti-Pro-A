package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckUserResponse(
    val exists: Boolean,
    @SerialName("first_name") val firstName: String?
)
