package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.UserStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PasswordBasedSignUpResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val userName: String,
    val status: UserStatus
)
