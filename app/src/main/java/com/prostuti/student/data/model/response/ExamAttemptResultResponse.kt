package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.AttemptMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamAttemptResultResponse(
    @SerialName("id")
    val id: String,
    @SerialName("exam_id")
    val examId: String,
    @SerialName("student_id")
    val studentId: String,
    @SerialName("mode")
    val mode: AttemptMode,
    @SerialName("start_at")
    val startAt: String,
    @SerialName("end_at")
    val endAt: String? = null,
    @SerialName("right_answer_count")
    val rightAnswerCount: Int,
    @SerialName("wrong_answer_count")
    val wrongAnswerCount: Int,
    @SerialName("skip_count")
    val skipCount: Int,
    @SerialName("total_score")
    val totalScore: Double,
    @SerialName("rank")
    val rank: Int? = null
)
