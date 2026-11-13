package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** One inbox entry. [type] and [targetUrl] are free-form: unknown values must be ignored. */
@Serializable
data class NotificationItem(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("message") val message: String,
    @SerialName("type") val type: String,
    @SerialName("target_url") val targetUrl: String? = null,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String,
)
