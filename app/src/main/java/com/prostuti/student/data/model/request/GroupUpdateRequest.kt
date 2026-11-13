package com.prostuti.student.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Body of `PATCH /groups/{id}`. Only non-null fields are sent: a property left at its default (null)
 * is not encoded, so the server keeps that field as it is.
 */
@Serializable
data class GroupUpdateRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("max_members") val maxMembers: Int? = null,
)
