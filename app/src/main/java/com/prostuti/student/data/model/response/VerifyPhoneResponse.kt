package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.UserStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyPhoneResponse(
    val status: UserStatus,
    @SerialName("is_phone_verified") val isPhoneVerified: Boolean
)
