package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogInResponse(
    @SerialName("access_token") val accessToken: String,
    val email: String? = null,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("first_name") val firstName: String,
    @SerialName("is_email_verified") val isEmailVerified: Boolean,
    @SerialName("is_phone_verified") val isPhoneVerified: Boolean,
    @SerialName("last_name") val lastName: String,
    @SerialName("refresh_token") val refreshToken: String,
    val role: UserRole,
    val status: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("user_id") val userId: String,
    val username: String
)
