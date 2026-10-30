package com.prostuti.student.data.service.local

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * One row per (examId) — both the in-progress answer scratchpad (updated as the student answers)
 * and, once [submittedAtEpochMillis] is set, the final submission snapshot. Keeping both in one
 * row is what makes the "app closes mid-exam → auto-submit on return" behavior possible: whatever
 * was last saved here is exactly what gets auto-submitted, same as a real crash-recovery would.
 */
@Entity(tableName = "timed_exam_attempt", primaryKeys = ["exam_id"])
data class TimedExamAttemptEntity(
    @ColumnInfo(name = "exam_id") val examId: String,
    @ColumnInfo(name = "kind") val kind: String,
    // "mcqId:optionId|mcqId:optionId|..." — ordered by answer time, last one wins per mcqId.
    @ColumnInfo(name = "answers_csv") val answersCsv: String = "",
    @ColumnInfo(name = "started_at") val startedAtEpochMillis: Long,
    @ColumnInfo(name = "submitted_at") val submittedAtEpochMillis: Long? = null,
    @ColumnInfo(name = "correct_count") val correctCount: Int = 0,
    @ColumnInfo(name = "wrong_count") val wrongCount: Int = 0,
    @ColumnInfo(name = "unanswered_count") val unansweredCount: Int = 0,
    @ColumnInfo(name = "total_marks") val totalMarks: Float = 0f,
    @ColumnInfo(name = "time_taken_seconds") val timeTakenSeconds: Int = 0,
    @ColumnInfo(name = "auto_submitted") val autoSubmitted: Boolean = false
)
