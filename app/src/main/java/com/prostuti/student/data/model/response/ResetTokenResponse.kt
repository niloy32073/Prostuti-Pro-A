package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Returned after the password-reset OTP is verified; short-lived, spent by [com.prostuti.student.data.model.request.ResetPasswordRequest]. */
@Serializable
data class ResetTokenResponse(
    @SerialName("reset_token") val resetToken: String
)
