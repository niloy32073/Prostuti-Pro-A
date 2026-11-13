package com.prostuti.student.data.model.request

import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.enums.QuestionDifficulty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Body of `POST /routines/slots` — a one-off group exam outside any routine (group admin only). */
@Serializable
data class AdhocExamRequest(
    @SerialName("subject_ids") val subjectIds: List<String>,
    /** Narrow to these chapters; empty = whole subjects. */
    @SerialName("chapter_ids") val chapterIds: List<String> = emptyList(),
    /** Narrow to these topics; empty = whole chapters. */
    @SerialName("topic_ids") val topicIds: List<String> = emptyList(),
    @SerialName("questions_count") val questionsCount: Int,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("mark_per_question") val markPerQuestion: Double = 1.0,
    @SerialName("negative_marking") val negativeMarking: Boolean = false,
    @SerialName("negative_mark_value") val negativeMarkValue: Double = 0.0,
    @SerialName("level") val level: Level? = null,
    @SerialName("difficulty") val difficulty: QuestionDifficulty? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("group_id") val groupId: String,
    /** The exam day, `yyyy-MM-dd` (Asia/Dhaka). */
    @SerialName("exam_date") val examDate: String,
    @SerialName("window_open_minute") val windowOpenMinute: Int,
    @SerialName("window_close_minute") val windowCloseMinute: Int,
    @SerialName("title") val title: String? = null,
)
