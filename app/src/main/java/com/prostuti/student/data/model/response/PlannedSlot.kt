package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** One day of a routine preview (not saved yet). */
@Serializable
data class PlannedSlot(
    @SerialName("slot_date") val slotDate: String,
    @SerialName("opens_at") val opensAt: String,
    @SerialName("closes_at") val closesAt: String,
    @SerialName("topic_ids") val topicIds: List<String> = emptyList(),
    @SerialName("topic_grouping") val topicGrouping: TopicGrouping,
)
