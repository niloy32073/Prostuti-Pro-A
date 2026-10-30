package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.TimedExam

interface LiveExamRepository {
    suspend fun liveExamList(): ApiResponse<List<TimedExam>>
    suspend fun liveExamDetail(examId: String): ApiResponse<TimedExam>
}
