package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

/** Shared by live and group exams — derived purely from the exam window and whether a
 * submission exists for it (see [com.prostuti.student.domain.TimedExamStateDeriver]). Never
 * stored directly: it's always recomputed from the window + submission, like the real backend
 * would. */
@Serializable
enum class TimedExamState {
    UPCOMING, OPEN_NOT_SUBMITTED, OPEN_SUBMITTED, CLOSED
}
