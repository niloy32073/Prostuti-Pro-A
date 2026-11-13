package com.prostuti.student.data.model.request

import com.prostuti.student.data.model.enums.GroupRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Body of `PATCH /groups/{id}/members/{user_id}`. */
@Serializable
data class MemberRoleRequest(@SerialName("role") val role: GroupRole)
