package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.request.ExamGenerateRequest
import com.prostuti.student.data.model.request.ExamHistoryFilter
import com.prostuti.student.data.model.request.ExamResponseSubmitRequest
import com.prostuti.student.data.model.request.IntegrityEventSubmitRequest
import com.prostuti.student.data.model.response.AnswerSheetData
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.AttemptAnalysisData
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.model.response.ExamHistoryItem
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.model.response.ExamQuestionWithOptions
import com.prostuti.student.data.model.response.ExamSummary
import com.prostuti.student.data.model.response.IntegrityEventResponse
import com.prostuti.student.data.model.response.MCQExam

interface ExamRepository {
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
    suspend fun listMyAttempts(examId: String?, unfinished: Boolean, page: Int, limit: Int): ApiResponse<List<ExamAttemptResultResponse>>
    suspend fun getAttemptResponses(attemptId: String): ApiResponse<List<ExamResponseAckResponse>>
    suspend fun getExam(examId: String): ApiResponse<ExamSummary>
    suspend fun submitIntegrityEvents(attemptId: String, request: IntegrityEventSubmitRequest): ApiResponse<List<IntegrityEventResponse>>
    suspend fun getExamHistory(filter: ExamHistoryFilter, page: Int, limit: Int): ApiResponse<List<ExamHistoryItem>>
    suspend fun getAnswerSheet(attemptId: String): ApiResponse<AnswerSheetData>
    suspend fun getAttemptAnalysis(attemptId: String): ApiResponse<AttemptAnalysisData>
    suspend fun getExamLeaderboard(examId: String, page: Int): ApiResponse<ExamLeaderboardData>
    suspend fun getExamQuestionsWithOptions(examId: String): ApiResponse<List<ExamQuestionWithOptions>>
}
