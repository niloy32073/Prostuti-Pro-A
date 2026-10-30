package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.repository.TimedExamRepository

/** Backs the debug-only "ডেমো: অবস্থা পাল্টাও" chip on GroupDetail/LiveExamDetail. */
class AdvanceDemoStateUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String, kind: TimedExamKind) =
        repository.advanceDemoState(examId, kind)
}
