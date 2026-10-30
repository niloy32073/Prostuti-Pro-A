package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.data.repository.MCQExamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetExamLeaderboardUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(examId: String, page: Int = 1): ApiResponse<ExamLeaderboardData> =
        withContext(Dispatchers.IO) { repository.getExamLeaderboard(examId, page) }
}
