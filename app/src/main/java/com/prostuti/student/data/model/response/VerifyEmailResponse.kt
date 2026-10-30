package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.UserStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyEmailResponse(
    val status: UserStatus,
    @SerialName("is_email_verified") val isEmailVerified: Boolean
)
