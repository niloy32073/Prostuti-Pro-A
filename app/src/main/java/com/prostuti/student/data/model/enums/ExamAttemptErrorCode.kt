package com.prostuti.student.data.model.enums

/** `error.code` values the exam-attempt API returns that the app branches on. */
object ExamAttemptErrorCode {
    const val ATTEMPT_ALREADY_ACTIVE = "ATTEMPT_ALREADY_ACTIVE"
    const val ATTEMPT_ALREADY_ENDED = "ATTEMPT_ALREADY_ENDED"
    const val EXAM_EXPIRED = "EXAM_EXPIRED"

    /** App-side code: an unfinished LIVE attempt was found on start and submitted, since live
     *  exams can't be resumed. Not sent by the server. */
    const val LIVE_ATTEMPT_SUBMITTED = "LIVE_ATTEMPT_SUBMITTED"
}
