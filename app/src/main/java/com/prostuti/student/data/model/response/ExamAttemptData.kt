package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamAttemptData(
    @SerialName("attempt_id")
    val attemptId: String,
    @SerialName("duration")
    val duration: Int,
    @SerialName("exam_id")
    val examId: String,
    @SerialName("questions")
    val questions: List<Question>,
    @SerialName("start_at")
    val startAt: String
)

@Serializable
data class Question(
    @SerialName("mcq_id")
    val mcqId: String,
    @SerialName("options")
    val options: List<Option>,
    @SerialName("order")
    val order: Int,
    @SerialName("passage_id")
    val passageId: String? = null,
    @SerialName("question_text")
    val questionText: String? = null,
    @SerialName("question_image_link")
    val questionImageLink: String? = null
)

@Serializable
data class Option(
    @SerialName("id")
    val id: String,
    @SerialName("option_text")
    val optionText: String? = null,
    @SerialName("option_image_link")
    val optionImageLink: String? = null,
    @SerialName("order")
    val order: Int
)
