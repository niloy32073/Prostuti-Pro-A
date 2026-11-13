package com.prostuti.student.data.model.enums

import com.prostuti.student.util.FallbackEnumSerializer
import kotlinx.serialization.Serializable


/** How to label one exam's topics: one chapter, several chapters of one subject, or just the topics. */
@Serializable(with = TopicGroupingLevelSerializer::class)
enum class TopicGroupingLevel {
    CHAPTER, CHAPTERS, TOPICS
}

object TopicGroupingLevelSerializer :
    FallbackEnumSerializer<TopicGroupingLevel>("TopicGroupingLevel", TopicGroupingLevel.entries, TopicGroupingLevel.TOPICS)
