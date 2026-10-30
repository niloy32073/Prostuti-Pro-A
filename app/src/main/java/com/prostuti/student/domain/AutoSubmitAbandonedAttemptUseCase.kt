package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.repository.TimedExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Called when a timed-exam screen opens, before showing any question — if the student closed
 * the app mid-exam last time, whatever was saved gets submitted right away and this returns that
 * submission so the caller can skip straight to the locked-result screen. */
class AutoSubmitAbandonedAttemptUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String, kind: TimedExamKind): MyTimedExamSubmission? =
        withContext(Dispatchers.IO) { repository.autoSubmitAbandonedAttempt(examId, kind) }
}
