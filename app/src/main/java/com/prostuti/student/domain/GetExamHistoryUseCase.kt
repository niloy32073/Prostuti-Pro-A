package com.prostuti.student.domain

import com.prostuti.student.data.model.request.ExamHistoryFilter
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamHistoryItem
import com.prostuti.student.data.repository.ExamRepository

/** One page of the profile exam history (finished + missed attempts, newest first). */
class GetExamHistoryUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(
        filter: ExamHistoryFilter,
        page: Int,
        limit: Int = PAGE_SIZE
    ): ApiResponse<List<ExamHistoryItem>> = repository.getExamHistory(filter, page, limit)

    companion object {
        const val PAGE_SIZE = 20
    }
}
