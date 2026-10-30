package com.prostuti.student.data.model.response

import kotlinx.serialization.Serializable

@Serializable
data class SendOTPResponse(
    val otp: String
)
