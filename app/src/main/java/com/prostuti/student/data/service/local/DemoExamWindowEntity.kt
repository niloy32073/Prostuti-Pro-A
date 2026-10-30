package com.prostuti.student.data.service.local

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * The resolved, absolute start/end timestamp for one dummy exam's window. [ExamDummyData]'s
 * baseline offsets (e.g. "opens 1 hour ago, closes in 3 hours") are only ever applied ONCE, the
 * first time that exam is looked up — the result is anchored here so the window keeps counting
 * down in real time and survives the app being closed and reopened, instead of re-rolling itself
 * relative to "now" on every launch.
 */
@Entity(tableName = "demo_exam_window", primaryKeys = ["exam_id"])
data class DemoExamWindowEntity(
    @ColumnInfo(name = "exam_id") val examId: String,
    @ColumnInfo(name = "window_start") val windowStartEpochMillis: Long,
    @ColumnInfo(name = "window_end") val windowEndEpochMillis: Long
)
