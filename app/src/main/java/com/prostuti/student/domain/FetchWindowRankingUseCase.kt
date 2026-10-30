package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.WindowRanking
import com.prostuti.student.data.repository.TimedExamRepository

class FetchWindowRankingUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): ApiResponse<WindowRanking> =
        repository.windowRanking(examId)
}
