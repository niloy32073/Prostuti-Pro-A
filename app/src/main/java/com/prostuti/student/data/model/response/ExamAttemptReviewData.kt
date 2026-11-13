package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One answer-sheet question as the answer-sheet screen shows it (built by `mergeAnswerSheet`). */
@Serializable
data class MCQReviewQuestionResponse(
    @SerialName("mcq_id")
    val mcqId: String,
    @SerialName("order")
    val order: Int,
    @SerialName("question_text")
    val questionText: String? = null,
    @SerialName("question_image_link")
    val questionImageLink: String? = null,
    @SerialName("explanation_text")
    val explanationText: String? = null,
    @SerialName("explanation_image_link")
    val explanationImageLink: String? = null,
    @SerialName("options")
    val options: List<MCQReviewOptionResponse>,
    @SerialName("selected_option_id")
    val selectedOptionId: String? = null,
    @SerialName("is_correct")
    val isCorrect: Boolean? = null,
    /** The shared passage (উদ্দীপক) this question belongs to, if any. */
    @SerialName("passage")
    val passage: FetchPassageData? = null
)

@Serializable
data class MCQReviewOptionResponse(
    @SerialName("id")
    val id: String,
    @SerialName("option_text")
    val optionText: String? = null,
    @SerialName("option_image_link")
    val optionImageLink: String? = null,
    @SerialName("order")
    val order: Int,
    @SerialName("is_correct")
    val isCorrect: Boolean
)
