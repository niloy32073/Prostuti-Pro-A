package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.JoinRequestStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** A request to join a public group. */
@Serializable
data class GroupJoinRequest(
    @SerialName("id") val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String? = null,
    @SerialName("status") val status: JoinRequestStatus,
    @SerialName("created_at") val createdAt: String,
    @SerialName("decided_at") val decidedAt: String? = null,
)
