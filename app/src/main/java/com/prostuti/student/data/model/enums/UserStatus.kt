package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class UserStatus {
    ACTIVE, BLOCKED, PENDING, UNVERIFIED
}
