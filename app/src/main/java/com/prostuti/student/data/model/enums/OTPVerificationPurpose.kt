package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class OTPVerificationPurpose {
    EMAIL_VERIFY, PHONE_VERIFY, PASSWORD_RESET, MAGIC_LOGIN
}
