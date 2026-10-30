package com.prostuti.student.data.service.local

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * The routine an admin creates for a group (the 3-step Subject → Chapter → Window flow). Local
 * only — this is the fake/dummy data layer's own persistence, not a mirror of a real API model,
 * so it isn't `@Serializable`. Subject/chapter/topic ids are stored as comma-joined strings
 * rather than needing a Room [androidx.room.TypeConverter] for a tiny bit of local state.
 */
@Entity(tableName = "group_routine", primaryKeys = ["group_id"])
data class GroupRoutineEntity(
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "subject_ids_csv") val subjectIdsCsv: String,
    @ColumnInfo(name = "chapter_ids_csv") val chapterIdsCsv: String,
    @ColumnInfo(name = "topic_ids_csv") val topicIdsCsv: String,
    @ColumnInfo(name = "routine_duration_days") val routineDurationDays: Int,
    // Bit i (0=সোম..6=রবি) set when the group has an exam that weekday.
    @ColumnInfo(name = "exam_week_days_bitmask") val examWeekDaysBitmask: Int,
    @ColumnInfo(name = "window_start_minute_of_day") val windowStartMinuteOfDay: Int,
    @ColumnInfo(name = "window_end_minute_of_day") val windowEndMinuteOfDay: Int,
    @ColumnInfo(name = "question_count") val questionCount: Int,
    @ColumnInfo(name = "mark_per_question") val markPerQuestion: Float,
    @ColumnInfo(name = "negative_marking_enabled") val negativeMarkingEnabled: Boolean,
    @ColumnInfo(name = "negative_mark") val negativeMark: Float,
    @ColumnInfo(name = "exam_duration_minutes") val examDurationMinutes: Int,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long
)
