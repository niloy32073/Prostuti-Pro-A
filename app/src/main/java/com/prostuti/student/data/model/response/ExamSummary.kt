package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The few fields of `GET /exams/{exam_id}` that Home's continue card needs. Kept separate from
 * [MCQExam] on purpose: generated practice exams can differ in fields this card never reads
 * (exam_type, exam_year, …), and a mismatch there must not hide the card's title.
 */
@Serializable
data class ExamSummary(
    @SerialName("id")
    val id: String,
    @SerialName("title_bn")
    val titleBn: String? = null,
    @SerialName("title_en")
    val titleEn: String? = null,
    @SerialName("duration")
    val duration: Int? = null
)
