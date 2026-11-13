package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.SlotKind
import com.prostuti.student.data.model.enums.SlotMyStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * One scheduled exam (a routine day or a one-off group exam). [opensAt]/[closesAt] are UTC
 * timestamps (parse with `DhakaTime.parseInstant`); [slotDate] is the Dhaka day. [examId] is null
 * until someone first starts the slot.
 */
@Serializable
data class RoutineSlot(
    @SerialName("id") val id: String,
    @SerialName("kind") val kind: SlotKind,
    @SerialName("routine_id") val routineId: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("slot_date") val slotDate: String,
    @SerialName("opens_at") val opensAt: String,
    @SerialName("closes_at") val closesAt: String,
    @SerialName("topic_ids") val topicIds: List<String> = emptyList(),
    @SerialName("topic_grouping") val topicGrouping: TopicGrouping,
    @SerialName("questions_count") val questionsCount: Int,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("mark_per_question") val markPerQuestion: Double,
    /** 0 = no negative marking. */
    @SerialName("negative_mark_value") val negativeMarkValue: Double,
    @SerialName("level") val level: String? = null,
    @SerialName("difficulty") val difficulty: String? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("postponed_count") val postponedCount: Int = 0,
    @SerialName("exam_id") val examId: String? = null,
    @SerialName("my_status") val myStatus: SlotMyStatus,
)
