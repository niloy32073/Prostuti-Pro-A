package com.prostuti.student.domain

import com.prostuti.student.data.model.request.IntegrityEventSubmitRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.IntegrityEventResponse
import com.prostuti.student.data.repository.MCQExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubmitIntegrityEventsUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(attemptId: String, request: IntegrityEventSubmitRequest): ApiResponse<List<IntegrityEventResponse>> =
        withContext(Dispatchers.IO) { repository.submitIntegrityEvents(attemptId, request) }
}
