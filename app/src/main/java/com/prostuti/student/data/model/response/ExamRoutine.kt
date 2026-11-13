package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.Weekday
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** A saved routine's settings. [groupId] is null for a personal routine. */
@Serializable
data class ExamRoutine(
    @SerialName("id") val id: String,
    @SerialName("group_id") val groupId: String? = null,
    /** ACTIVE or DELETED. */
    @SerialName("status") val status: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("duration_days") val durationDays: Int,
    @SerialName("weekdays") val weekdays: List<Weekday> = emptyList(),
    @SerialName("subject_ids") val subjectIds: List<String> = emptyList(),
    @SerialName("chapter_ids") val chapterIds: List<String> = emptyList(),
    @SerialName("topic_ids") val topicIds: List<String> = emptyList(),
    @SerialName("window_open_minute") val windowOpenMinute: Int,
    @SerialName("window_close_minute") val windowCloseMinute: Int,
    @SerialName("questions_count") val questionsCount: Int,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("mark_per_question") val markPerQuestion: Double,
    @SerialName("negative_mark_value") val negativeMarkValue: Double,
    @SerialName("level") val level: String? = null,
    @SerialName("difficulty") val difficulty: String? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("created_at") val createdAt: String,
)
