package com.prostuti.student.domain

import com.prostuti.student.data.model.request.ExamResponseSubmitRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.repository.MCQExamRepository

class SubmitExamResponsesUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(attemptId: String, request: ExamResponseSubmitRequest): ApiResponse<List<ExamResponseAckResponse>> =
        repository.submitResponses(attemptId, request)
}
