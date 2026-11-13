package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.TimedExamKind
import kotlinx.serialization.Serializable

/**
 * One timed, single-attempt exam occurrence — live or group, same shape. [windowStartEpochMillis]
 * and [windowEndEpochMillis] are already-resolved absolute timestamps (see `DemoExamWindowEntity`
 * in the fake data layer); nothing downstream of this model deals in relative offsets.
 */
@Serializable
data class TimedExam(
    val examId: String,
    val kind: TimedExamKind,
    val titleBn: String,
    val subtitleBn: String,
    val windowStartEpochMillis: Long,
    val windowEndEpochMillis: Long,
    val examDurationMinutes: Int,
    val questionCount: Int,
    val markPerQuestion: Float,
    val negativeMark: Float,
    val totalParticipants: Int,
    /** English title/subtitle; blank = show the Bangla one. */
    val titleEn: String = "",
    val subtitleEn: String = ""
)

/** Whatever's known about *my* attempt at a [TimedExam] right now — null fields mean "not
 * submitted yet" or "window still open" (both locked per the no-score-until-window-closes rule). */
@Serializable
data class MyTimedExamSubmission(
    val submittedAtEpochMillis: Long,
    val timeTakenSeconds: Int,
    val autoSubmitted: Boolean
)
