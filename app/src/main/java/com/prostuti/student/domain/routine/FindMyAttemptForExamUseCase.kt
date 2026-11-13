package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptResultResponse
import com.prostuti.student.data.repository.ExamRepository

/**
 * My latest submitted attempt on a scheduled exam (a slot's `exam_id`), used to open its result or
 * answer sheet — slots don't carry an attempt id. With [liveOnly] it ignores practice retakes (the
 * one that counts on the leaderboard). `Success(null)` = I haven't submitted one.
 */
class FindMyAttemptForExamUseCase(private val repository: ExamRepository) {
    suspend operator fun invoke(examId: String, liveOnly: Boolean = false): ApiResponse<ExamAttemptResultResponse?> =
        when (val r = repository.listMyAttempts(examId = examId, unfinished = false, page = 1, limit = MAX_ATTEMPTS)) {
            is ApiResponse.Success -> ApiResponse.Success(
                r.data
                    .filter { it.examId == examId && it.endAt != null }
                    .filter { !liveOnly || it.mode == AttemptMode.LIVE }
                    // ISO-8601 UTC strings of one shape sort chronologically.
                    .maxByOrNull { it.startAt }
            )
            is ApiResponse.Failure -> r
        }

    private companion object {
        const val MAX_ATTEMPTS = 50
    }
}
