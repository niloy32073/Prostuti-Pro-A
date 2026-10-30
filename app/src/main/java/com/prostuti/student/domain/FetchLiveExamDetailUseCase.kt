package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.TimedExam
import com.prostuti.student.data.repository.LiveExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchLiveExamDetailUseCase(private val repository: LiveExamRepository) {
    suspend operator fun invoke(examId: String): ApiResponse<TimedExam> =
        withContext(Dispatchers.IO) { repository.liveExamDetail(examId) }
}
