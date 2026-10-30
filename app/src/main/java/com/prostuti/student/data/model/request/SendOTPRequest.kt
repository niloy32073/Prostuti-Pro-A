package com.prostuti.student.data.model.request

import com.prostuti.student.data.model.enums.OTPVerificationPurpose
import kotlinx.serialization.Serializable

@Serializable
data class SendOTPRequest(
    val identifier: String,
    val purpose: OTPVerificationPurpose
)
