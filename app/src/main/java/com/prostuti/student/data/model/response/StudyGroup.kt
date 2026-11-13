package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.GroupRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** A group as the signed-in user sees it. [inviteCode] is filled in for the group's admins only;
 * [myRole] is null when the user isn't a member; [hasPendingRequest] is only set by Explore. */
@Serializable
data class StudyGroup(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("max_members") val maxMembers: Int,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("my_role") val myRole: GroupRole? = null,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("has_pending_request") val hasPendingRequest: Boolean = false,
)
