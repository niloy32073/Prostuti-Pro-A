package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** One chapter inside a [TopicGrouping]: this exam's topics in it, and whether that's only part of the chapter. */
@Serializable
data class ChapterGroup(
    @SerialName("chapter_id") val chapterId: String,
    @SerialName("topic_ids") val topicIds: List<String> = emptyList(),
    @SerialName("is_partial") val isPartial: Boolean = false,
)
