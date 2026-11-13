package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

/**
 * What a bookmark points at (`resource_type` of `learning/bookmarks`). The data layer stores and
 * syncs every type; the app shows MCQ bookmarks for now. To add another type, call the bookmark
 * use cases (or `BookmarkToggler`) with that type — no data-layer change is needed.
 */
@Serializable
enum class BookmarkResourceType {
    MCQ,
    CONCEPTUAL_QUESTION,
    DESCRIPTIVE_QUESTION,
    EBOOK
}
