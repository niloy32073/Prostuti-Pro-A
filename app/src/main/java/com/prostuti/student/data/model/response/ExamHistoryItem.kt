package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of `GET exams/attempts/history`: a finished attempt, or a missed scheduled slot.
 * A MISSED row has all-zero counts — show it as "missed", never as "all skipped".
 * [status] and [mode] stay plain strings so a value the app doesn't know yet can't break parsing.
 */
@Serializable
data class ExamHistoryItem(
    @SerialName("exam_id")
    val examId: String,
    @SerialName("attempt_id")
    val attemptId: String,
    @SerialName("exam_title_en")
    val examTitleEn: String = "",
    @SerialName("exam_title_bn")
    val examTitleBn: String = "",
    @SerialName("attempted_at")
    val attemptedAt: String,
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("correct_count")
    val correctCount: Int = 0,
    @SerialName("wrong_count")
    val wrongCount: Int = 0,
    @SerialName("skipped_count")
    val skippedCount: Int = 0,
    @SerialName("total_score")
    val totalScore: Double = 0.0,
    @SerialName("subject_ids")
    val subjectIds: List<String> = emptyList()
)
