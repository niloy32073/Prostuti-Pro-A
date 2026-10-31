package com.prostuti.student.data.service.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prostuti.student.data.model.response.MeData

/**
 * Cached copy of `GET auth/me` — at most one row (the signed-in user). The denied lists are
 * not cached: the app never asks for them.
 */
@Entity(tableName = "current_user")
data class CurrentUserEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "profile_image") val profileImage: String?,
    @ColumnInfo(name = "gender") val gender: String?,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "provider") val provider: String,
    @ColumnInfo(name = "is_email_verified") val isEmailVerified: Boolean,
    @ColumnInfo(name = "is_phone_verified") val isPhoneVerified: Boolean,
    @ColumnInfo(name = "last_login_at") val lastLoginAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "scopes") val scopes: List<String>
)

fun MeData.toEntity() = CurrentUserEntity(
    userId = userId, firstName = firstName, lastName = lastName, username = username,
    email = email, phone = phone, profileImage = profileImage, gender = gender,
    role = role, status = status, provider = provider,
    isEmailVerified = isEmailVerified, isPhoneVerified = isPhoneVerified,
    lastLoginAt = lastLoginAt, createdAt = createdAt, scopes = scopes
)

fun CurrentUserEntity.toMeData() = MeData(
    userId = userId, firstName = firstName, lastName = lastName, username = username,
    email = email, phone = phone, profileImage = profileImage, gender = gender,
    role = role, status = status, provider = provider,
    isEmailVerified = isEmailVerified, isPhoneVerified = isPhoneVerified,
    lastLoginAt = lastLoginAt, createdAt = createdAt, scopes = scopes
)
