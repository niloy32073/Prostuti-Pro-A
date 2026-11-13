package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.McqDetail
import com.prostuti.student.data.repository.MCQRepository

class FetchMcqUseCase(private val repository: MCQRepository) {
    suspend operator fun invoke(mcqId: String): ApiResponse<McqDetail> = repository.fetchMcq(mcqId)
}
