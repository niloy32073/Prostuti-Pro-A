package com.prostuti.student.data.model.request

import kotlinx.serialization.Serializable

@Serializable
data class VerifyIdentifierRequest(
    val identifier: String,
    val code: String
)
