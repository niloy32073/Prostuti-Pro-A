package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.UserStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PasswordBasedSignUpResponse(
    @SerialName("access_token") val accessToken: String,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("refresh_token") val refreshToken: String,
    val status: UserStatus,
    @SerialName("token_type") val tokenType: String,
    @SerialName("user_id") val userId: String,
    val username: String
)
