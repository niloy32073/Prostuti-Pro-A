package com.prostuti.student.data.model.response

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * `GET exams/attempts/{attempt_id}/answer-sheet` — frozen at submit time. It carries IDs and the
 * result only; question and option text come from `GET exams/{exam_id}/questions/with-options`.
 */
@Serializable
data class AnswerSheetData(
    @SerialName("attempt_id")
    val attemptId: String,
    @SerialName("exam_id")
    val examId: String,
    @SerialName("source")
    val source: String? = null,
    @SerialName("questions")
    val questions: List<AnswerSheetQuestion> = emptyList()
)

/** [result] is CORRECT, WRONG or SKIPPED (see `AnswerResult`). [selectedOptionId] null = skipped. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AnswerSheetQuestion(
    @SerialName("mcq_id")
    val mcqId: String,
    @SerialName("order")
    val order: Int,
    @SerialName("subject_id")
    val subjectId: String? = null,
    @SerialName("chapter_id")
    val chapterId: String? = null,
    @SerialName("topic_id")
    val topicId: String? = null,
    @SerialName("selected_option_id")
    val selectedOptionId: String? = null,
    @SerialName("correct_option_id")
    val correctOptionId: String? = null,
    @SerialName("result")
    val result: String,
    @SerialName("is_correct")
    val isCorrect: Boolean = false,
    @SerialName("explanation_text")
    val explanationText: String? = null,
    // Not in the OpenAPI schema yet, but the backend sends images; both names are accepted.
    @SerialName("explanation_image_link")
    @JsonNames("explanation_image")
    val explanationImageLink: String? = null
)
