package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.request.ExamGenerateRequest
import com.prostuti.student.data.model.request.ExamResponseSubmitRequest
import com.prostuti.student.data.model.request.IntegrityEventSubmitRequest
import com.prostuti.student.data.model.request.McqReportRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.model.response.ExamAttemptReviewData
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.IntegrityEventResponse
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.data.model.response.McqReportResponse

interface MCQExamRepository {
    suspend fun fetchMCQExams(
        subjectId: String,
        examType: ExamTypes,
        level: Level,
        page: Int,
        limit: Int
    ): ApiResponse<List<MCQExam>>

    suspend fun generateExam(request: ExamGenerateRequest): ApiResponse<ExamAttemptData>
    suspend fun attemptMCQExam(examId: String): ApiResponse<ExamAttemptData>
    suspend fun getAttemptQuestions(attemptId: String): ApiResponse<ExamAttemptData>
    suspend fun submitResponses(attemptId: String, request: ExamResponseSubmitRequest): ApiResponse<List<ExamResponseAckResponse>>
    suspend fun submitAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse>
    suspend fun getAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse>
    suspend fun listMyAttempts(examId: String?, page: Int, limit: Int): ApiResponse<List<ExamAttemptResultResponse>>
    suspend fun submitIntegrityEvents(attemptId: String, request: IntegrityEventSubmitRequest): ApiResponse<List<IntegrityEventResponse>>
    suspend fun getAttemptReview(attemptId: String): ApiResponse<ExamAttemptReviewData>
    suspend fun getExamLeaderboard(examId: String, page: Int): ApiResponse<ExamLeaderboardData>
    suspend fun fetchPassage(passageId: String): ApiResponse<FetchPassageData>
    suspend fun reportMcq(mcqId: String, request: McqReportRequest): ApiResponse<McqReportResponse>
}
