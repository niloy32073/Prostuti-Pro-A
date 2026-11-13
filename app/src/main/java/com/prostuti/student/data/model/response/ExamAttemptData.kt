package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.AttemptMode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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
    val startAt: String,
    // Not sent by the backend yet; until it is, every attempt opened here is treated as practice.
    // LIVE attempts are still caught from the manage list (Home, QB resume), which has the mode.
    @SerialName("mode")
    val mode: AttemptMode = AttemptMode.PRACTICE
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
    val questionImageLink: String? = null,
    // Position inside its passage group; null when the API doesn't send it.
    @SerialName("passage_order")
    val passageOrder: Int? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Option(
    @SerialName("id")
    val id: String,
    @SerialName("option_text")
    val optionText: String? = null,
    // The attempt API sends "option_image"; the older name is still accepted.
    @SerialName("option_image")
    @JsonNames("option_image_link")
    val optionImageLink: String? = null,
    @SerialName("order")
    val order: Int,
    // Only sent by the questions-with-options API; null when the answer isn't shared.
    @SerialName("is_correct")
    val isCorrect: Boolean? = null
)
