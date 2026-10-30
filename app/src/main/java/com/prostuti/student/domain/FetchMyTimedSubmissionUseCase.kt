package com.prostuti.student.domain

import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.repository.TimedExamRepository

class FetchMyTimedSubmissionUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): MyTimedExamSubmission? =
        repository.mySubmission(examId)
}
