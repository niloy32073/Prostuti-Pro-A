package com.prostuti.student.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Body of `POST /groups`. Name 2-255, description up to 1024, member cap 2-5000. */
@Serializable
data class GroupCreateRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("max_members") val maxMembers: Int = 500,
)
