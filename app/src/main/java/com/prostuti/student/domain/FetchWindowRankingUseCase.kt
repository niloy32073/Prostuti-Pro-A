package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.WindowRanking
import com.prostuti.student.data.repository.TimedExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FetchWindowRankingUseCase(private val repository: TimedExamRepository) {
    suspend operator fun invoke(examId: String): ApiResponse<WindowRanking> =
        withContext(Dispatchers.IO) { repository.windowRanking(examId) }
}
