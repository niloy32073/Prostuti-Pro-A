package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.enums.ExamAttemptErrorCode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.repository.MCQExamRepository

/**
 * Starts an attempt on [examId]. If the student already has an unfinished one
 * (`ATTEMPT_ALREADY_ACTIVE`), finds it and reloads its questions instead — the API treats that
 * case as "resume", not as a failure. Exception: a LIVE attempt can't be resumed, so it is
 * submitted and [ExamAttemptErrorCode.LIVE_ATTEMPT_SUBMITTED] is returned.
 */
class StartOrResumeExamAttemptUseCase(private val repository: MCQExamRepository) {
    suspend operator fun invoke(examId: String): ApiResponse<ExamAttemptData> {
        val started = repository.attemptMCQExam(examId)
        if (started !is ApiResponse.Failure || started.errorCode != ExamAttemptErrorCode.ATTEMPT_ALREADY_ACTIVE) {
            return started
        }
        return when (val list = repository.listMyAttempts(examId = examId, unfinished = true, page = 1, limit = 1)) {
            is ApiResponse.Success -> {
                val unfinished = list.data.firstOrNull() ?: return started
                if (unfinished.mode == AttemptMode.LIVE) {
                    repository.submitAttempt(unfinished.id)
                    ApiResponse.Failure(
                        message = "তোমার আগের লাইভ পরীক্ষাটি জমা দেওয়া হয়েছে। লাইভ পরীক্ষা আবার শুরু করা যায় না।",
                        errorCode = ExamAttemptErrorCode.LIVE_ATTEMPT_SUBMITTED
                    )
                } else {
                    // Mode from the list is reliable; the questions payload doesn't carry it yet.
                    when (val q = repository.getAttemptQuestions(unfinished.id)) {
                        is ApiResponse.Success -> q.copy(data = q.data.copy(mode = unfinished.mode))
                        is ApiResponse.Failure -> q
                    }
                }
            }
            is ApiResponse.Failure -> list
        }
    }
}
