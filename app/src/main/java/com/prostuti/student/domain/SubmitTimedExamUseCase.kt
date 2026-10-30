package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.repository.TimedExamRepository

class SubmitTimedExamUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String, kind: TimedExamKind): ApiResponse<MyTimedExamSubmission> =
        repository.submit(examId, kind)
}
