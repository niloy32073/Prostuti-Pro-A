package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.repository.TimedExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubmitTimedExamUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String, kind: TimedExamKind): ApiResponse<MyTimedExamSubmission> =
        withContext(Dispatchers.IO) { repository.submit(examId, kind) }
}
