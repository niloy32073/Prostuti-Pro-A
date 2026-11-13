package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET exams/attempts/{attempt_id}/analysis` — one entry per topic, weakest (most wrong) first. */
@Serializable
data class AttemptAnalysisData(
    @SerialName("attempt_id")
    val attemptId: String,
    @SerialName("exam_id")
    val examId: String,
    @SerialName("source")
    val source: String? = null,
    @SerialName("topics")
    val topics: List<TopicAnalysisItem> = emptyList()
)

/** [topicId] null groups the questions that have no topic. total = correct + wrong + skipped. */
@Serializable
data class TopicAnalysisItem(
    @SerialName("topic_id")
    val topicId: String? = null,
    @SerialName("total_questions")
    val totalQuestions: Int,
    @SerialName("correct")
    val correct: Int,
    @SerialName("wrong")
    val wrong: Int,
    @SerialName("skipped")
    val skipped: Int
)
