package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The signed-in user's profile from `GET auth/me`. `role`, `status` and `provider` stay plain
 * strings (the spec types them as string, and the backend has more roles than [UserRole] knows).
 * [deniedScopes]/[deniedRoles] only come back with `include_denied=true`.
 */
@Serializable
data class MeData(
    @SerialName("user_id") val userId: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("username") val username: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("profile_image") val profileImage: String? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("role") val role: String,
    @SerialName("status") val status: String,
    @SerialName("provider") val provider: String,
    @SerialName("is_email_verified") val isEmailVerified: Boolean,
    @SerialName("is_phone_verified") val isPhoneVerified: Boolean,
    @SerialName("last_login_at") val lastLoginAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("scopes") val scopes: List<String> = emptyList(),
    @SerialName("denied_scopes") val deniedScopes: List<String>? = null,
    @SerialName("denied_roles") val deniedRoles: List<String>? = null
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}
