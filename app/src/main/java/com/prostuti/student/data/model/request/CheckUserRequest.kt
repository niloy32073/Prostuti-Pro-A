package com.prostuti.student.data.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CheckUserRequest(
    val identifier: String
)
