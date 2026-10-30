package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MCQExam(
    @SerialName("id")
    val id: String,

    @SerialName("duration")
    val duration: Int,

    @SerialName("level")
    val level: Level,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("created_by")
    val createdBy: String? = null,

    @SerialName("description_bn")
    val descriptionBn: String? = null,

    @SerialName("description_en")
    val descriptionEn: String? = null,

    @SerialName("exam_type")
    val examType: ExamTypes,

    @SerialName("exam_year")
    val examYear: Int,

    @SerialName("expired_at")
    val expiredAt: String? = null,

    @SerialName("institute_id")
    val instituteId: String? = null,

    @SerialName("join_code")
    val joinCode: String? = null,

    @SerialName("mark_per_question")
    val markPerQuestion: Double,

    @SerialName("negative_mark_per_wrong_answer")
    val negativeMarkPerWrongAnswer: Double,

    @SerialName("question_count")
    val questionCount: Int,

    @SerialName("schedule_at")
    val scheduleAt: String? = null,

    @SerialName("show_explanation_after")
    val showExplanationAfter: String,

    @SerialName("shuffle_options")
    val shuffleOptions: Boolean,

    @SerialName("shuffle_questions")
    val shuffleQuestions: Boolean,

    @SerialName("title_bn")
    val titleBn: String,

    @SerialName("title_en")
    val titleEn: String
)
