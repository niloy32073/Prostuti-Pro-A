package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One bookmark from `learning/bookmarks` (create and list). */
@Serializable
data class Bookmark(
    @SerialName("id")
    val id: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("resource_type")
    val resourceType: String,
    @SerialName("resource_id")
    val resourceId: String,
    @SerialName("page_no")
    val pageNo: Int? = null,
    @SerialName("comments")
    val comments: String? = null,
    @SerialName("created_at")
    val createdAt: String
)
