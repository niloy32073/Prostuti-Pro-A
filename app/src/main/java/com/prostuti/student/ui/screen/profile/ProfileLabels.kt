package com.prostuti.student.ui.screen.profile

import androidx.annotation.StringRes
import com.prostuti.student.R

/** Display label for a backend role; null for a role this app doesn't know (shown as-is). */
@StringRes
fun roleLabelRes(role: String): Int? = when (role.uppercase()) {
    "STUDENT" -> R.string.role_student
    "TEACHER" -> R.string.role_teacher
    "ADMIN" -> R.string.role_admin
    "MODERATOR" -> R.string.role_moderator
    "REVIEWER" -> R.string.role_reviewer
    "DATAOP" -> R.string.role_dataop
    else -> null
}

/** Display label for the sign-in provider; null for an unknown one (shown as-is). */
@StringRes
fun providerLabelRes(provider: String): Int? = when (provider.uppercase()) {
    "EMAIL" -> R.string.provider_email
    "PHONE" -> R.string.provider_phone
    "GOOGLE" -> R.string.provider_google
    "FACEBOOK" -> R.string.provider_facebook
    "MAGIC_OTP" -> R.string.provider_magic_otp
    else -> null
}
