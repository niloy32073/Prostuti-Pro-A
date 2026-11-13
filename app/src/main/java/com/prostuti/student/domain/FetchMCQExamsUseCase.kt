package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.data.repository.ExamRepository

class FetchMCQExamsUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(
        subjectId: String,
        examType: ExamTypes,
        level: Level,
        page: Int = 1,
        limit: Int = 10
    ): ApiResponse<List<MCQExam>> = repository.fetchMCQExams(subjectId, examType, level, page, limit)
}
