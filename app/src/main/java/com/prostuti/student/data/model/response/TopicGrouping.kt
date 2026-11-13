package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.TopicGroupingLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** The server's display hint for an exam's topics. [subjectId] and [chapters] are empty for TOPICS. */
@Serializable
data class TopicGrouping(
    @SerialName("level") val level: TopicGroupingLevel,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("chapters") val chapters: List<ChapterGroup> = emptyList(),
)
