package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.repository.TimedExamRepository

/** Persists one answer choice to Room right away — the basis for auto-submitting an abandoned
 * attempt if the app is closed mid-exam (see [AutoSubmitAbandonedAttemptUseCase]). */
class SaveTimedExamAnswerUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String, kind: TimedExamKind, mcqId: String, optionId: String) =
        repository.saveAnswer(examId, kind, mcqId, optionId)
}
