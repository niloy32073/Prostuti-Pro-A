package com.prostuti.student.data.model.request

import com.prostuti.student.data.model.enums.Level
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamGenerateRequest(
    @SerialName("subject_ids")
    val subjectIds: List<String>? = null,
    // The backend requires chapter_ids whenever topic_ids is provided — always derive this as
    // the set of chapters that own the selected topics (never a user-independent chapter pick),
    // so the AND-across-filters match never excludes a selected topic.
    @SerialName("chapter_ids")
    val chapterIds: List<String>? = null,
    @SerialName("topic_ids")
    val topicIds: List<String>? = null,
    @SerialName("question_count")
    val questionCount: Int = 20,
    // duration/mark_per_question/negative_mark are not in the current backend contract — sent so
    // the backend can start reading them once support lands, without another client change.
    // Harmless if ignored. exclude_attempted is deliberately omitted: the backend already biases
    // generation away from previously-attempted questions on its own (UserMCQStatistics-weighted).
    @SerialName("duration")
    val durationMinutes: Int? = null,
    @SerialName("level")
    val level: Level? = null,
    @SerialName("version")
    val version: String? = null,
    @SerialName("mark_per_question")
    val markPerQuestion: Double? = null,
    @SerialName("negative_mark")
    val negativeMark: Double? = null
)
