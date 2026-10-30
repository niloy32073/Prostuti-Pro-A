package com.prostuti.student.domain

import com.prostuti.student.data.model.request.McqReportRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.McqReportResponse
import com.prostuti.student.data.repository.MCQExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubmitMcqReportUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(mcqId: String, reason: String, description: String? = null): ApiResponse<McqReportResponse> =
        withContext(Dispatchers.IO) {
            repository.reportMcq(mcqId, McqReportRequest(reason = reason, description = description))
        }
}
