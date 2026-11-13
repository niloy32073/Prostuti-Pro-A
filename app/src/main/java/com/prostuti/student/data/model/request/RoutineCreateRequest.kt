package com.prostuti.student.data.model.request

import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.enums.QuestionDifficulty
import com.prostuti.student.data.model.enums.Weekday
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Body of both `POST /routines/preview` and `POST /routines`. A personal routine leaves [groupId]
 * null and the window at its 24-hour default; a group routine (admin only) sets [groupId] and the
 * daily window in minutes after Dhaka midnight. Fields left at their default are not sent, and the
 * server applies the same defaults.
 */
@Serializable
data class RoutineCreateRequest(
    @SerialName("subject_ids") val subjectIds: List<String>,
    /** Narrow to these chapters; empty = whole subjects. */
    @SerialName("chapter_ids") val chapterIds: List<String> = emptyList(),
    /** Narrow to these topics; empty = whole chapters. */
    @SerialName("topic_ids") val topicIds: List<String> = emptyList(),
    @SerialName("questions_count") val questionsCount: Int,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("mark_per_question") val markPerQuestion: Double = 1.0,
    @SerialName("negative_marking") val negativeMarking: Boolean = false,
    @SerialName("negative_mark_value") val negativeMarkValue: Double = 0.0,
    @SerialName("level") val level: Level? = null,
    @SerialName("difficulty") val difficulty: QuestionDifficulty? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    /** How many days the routine runs, counting the start day (1-366). */
    @SerialName("duration_days") val durationDays: Int,
    @SerialName("weekdays") val weekdays: List<Weekday>,
    /** First day, `yyyy-MM-dd` (Asia/Dhaka); null = today. */
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("window_open_minute") val windowOpenMinute: Int = 0,
    @SerialName("window_close_minute") val windowCloseMinute: Int = 1440,
)
