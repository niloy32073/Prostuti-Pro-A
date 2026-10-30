package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    ADMIN, STUDENT, TEACHER, DATAOP, MODERATOR
}
