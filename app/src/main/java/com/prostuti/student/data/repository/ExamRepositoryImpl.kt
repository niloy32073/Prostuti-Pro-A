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
import com.prostuti.student.data.service.remote.ExamApiService

class ExamRepositoryImpl(
    private val apiService: ExamApiService
) : ExamRepository {

    override suspend fun fetchMCQExams(
        subjectId: String,
        examType: ExamTypes,
        level: Level,
        page: Int,
        limit: Int
    ): ApiResponse<List<MCQExam>> = apiService.fetchMCQExams(subjectId, examType, level, page, limit)

    override suspend fun generateExam(request: ExamGenerateRequest): ApiResponse<ExamAttemptData> =
        apiService.generateExam(request)

    override suspend fun attemptMCQExam(examId: String): ApiResponse<ExamAttemptData> =
        apiService.attemptMCQExam(examId)

    override suspend fun getAttemptQuestions(attemptId: String): ApiResponse<ExamAttemptData> =
        apiService.getAttemptQuestions(attemptId)

    override suspend fun submitResponses(
        attemptId: String,
        request: ExamResponseSubmitRequest
    ): ApiResponse<List<ExamResponseAckResponse>> = apiService.submitResponses(attemptId, request)

    override suspend fun submitAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> =
        apiService.submitAttempt(attemptId)

    override suspend fun getAttempt(attemptId: String): ApiResponse<ExamAttemptResultResponse> =
        apiService.getAttempt(attemptId)

    override suspend fun listMyAttempts(
        examId: String?,
        unfinished: Boolean,
        page: Int,
        limit: Int
    ): ApiResponse<List<ExamAttemptResultResponse>> = apiService.listMyAttempts(examId, unfinished, page, limit)

    override suspend fun getAttemptResponses(attemptId: String): ApiResponse<List<ExamResponseAckResponse>> =
        apiService.getAttemptResponses(attemptId)

    override suspend fun getExam(examId: String): ApiResponse<ExamSummary> = apiService.getExam(examId)

    override suspend fun submitIntegrityEvents(
        attemptId: String,
        request: IntegrityEventSubmitRequest
    ): ApiResponse<List<IntegrityEventResponse>> = apiService.submitIntegrityEvents(attemptId, request)

    override suspend fun getExamHistory(
        filter: ExamHistoryFilter,
        page: Int,
        limit: Int
    ): ApiResponse<List<ExamHistoryItem>> = apiService.getExamHistory(filter, page, limit)

    override suspend fun getAnswerSheet(attemptId: String): ApiResponse<AnswerSheetData> =
        apiService.getAnswerSheet(attemptId)

    override suspend fun getAttemptAnalysis(attemptId: String): ApiResponse<AttemptAnalysisData> =
        apiService.getAttemptAnalysis(attemptId)

    override suspend fun getExamLeaderboard(examId: String, page: Int): ApiResponse<ExamLeaderboardData> =
        apiService.getExamLeaderboard(examId, page)

    override suspend fun getExamQuestionsWithOptions(examId: String): ApiResponse<List<ExamQuestionWithOptions>> =
        apiService.getExamQuestionsWithOptions(examId)
}
