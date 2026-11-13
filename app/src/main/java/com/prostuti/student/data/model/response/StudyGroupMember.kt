package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.GroupRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** One row of `GET /groups/{id}/members` (admins come first). */
@Serializable
data class StudyGroupMember(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String,
    @SerialName("role") val role: GroupRole,
    @SerialName("joined_at") val joinedAt: String,
)
