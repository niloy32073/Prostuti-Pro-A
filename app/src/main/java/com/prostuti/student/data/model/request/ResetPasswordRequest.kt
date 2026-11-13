package com.prostuti.student.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Forgot-password, one call: the PASSWORD_RESET OTP sent to [identifier] plus the new password. */
@Serializable
data class ResetPasswordRequest(
    val identifier: String,
    val code: String,
    @SerialName("new_password") val newPassword: String
)
