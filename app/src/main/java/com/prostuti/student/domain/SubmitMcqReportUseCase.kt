package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.ReportRepository

class SubmitMcqReportUseCase(private val repository: ReportRepository) {
    suspend operator fun invoke(mcqId: String, reason: String, description: String? = null): ApiResponse<Unit> =
        repository.reportMcq(mcqId, reason, description)
}
