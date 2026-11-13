package com.prostuti.student.data.model.request

import com.prostuti.student.data.model.enums.BookmarkResourceType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of `POST learning/bookmarks`. */
@Serializable
data class BookmarkCreateRequest(
    @SerialName("resource_type")
    val resourceType: BookmarkResourceType,
    @SerialName("resource_id")
    val resourceId: String,
    @SerialName("page_no")
    val pageNo: Int? = null,
    @SerialName("comments")
    val comments: String? = null
)
