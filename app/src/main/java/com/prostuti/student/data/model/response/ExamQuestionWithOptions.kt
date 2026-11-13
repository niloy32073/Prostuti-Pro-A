package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One MCQ of an exam with its options and passage, from
 * `GET exams/{exam_id}/questions/with-options` (read without starting an attempt).
 * Students never get `is_correct` or explanations from this endpoint.
 */
@Serializable
data class ExamQuestionWithOptions(
    @SerialName("mcq_id")
    val mcqId: String,
    @SerialName("order")
    val order: Int,
    @SerialName("question_text")
    val questionText: String? = null,
    @SerialName("question_image_link")
    val questionImageLink: String? = null,
    @SerialName("passage_id")
    val passageId: String? = null,
    @SerialName("passage_order")
    val passageOrder: Int? = null,
    @SerialName("passage")
    val passage: FetchPassageData? = null,
    @SerialName("options")
    val options: List<Option> = emptyList()
) {
    fun toQuestion(): Question = Question(
        mcqId = mcqId,
        options = options,
        order = order,
        passageId = passageId,
        questionText = questionText,
        questionImageLink = questionImageLink,
        passageOrder = passageOrder
    )
}
