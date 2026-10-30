package com.prostuti.student.domain

import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.repository.TimedExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchMyTimedSubmissionUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): MyTimedExamSubmission? =
        withContext(Dispatchers.IO) { repository.mySubmission(examId) }
}
