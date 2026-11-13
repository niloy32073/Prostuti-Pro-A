package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.data.repository.ExamRepository

class GetExamLeaderboardUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(examId: String, page: Int = 1): ApiResponse<ExamLeaderboardData> =
        repository.getExamLeaderboard(examId, page)
}
