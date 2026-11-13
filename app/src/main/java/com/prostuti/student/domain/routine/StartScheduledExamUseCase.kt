package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.RoutineRepository
import com.prostuti.student.domain.StartOrResumeExamAttemptUseCase

/**
 * Takes a scheduled exam: `POST /routines/slots/{id}/start` (creates the shared exam on first use),
 * then starts an attempt on it through [StartOrResumeExamAttemptUseCase], so an unfinished attempt is
 * handled exactly like everywhere else in the exam engine. The attempt is LIVE inside the window and
 * PRACTICE after it.
 */
class StartScheduledExamUseCase(
    private val routineRepository: RoutineRepository,
    private val startOrResumeAttempt: StartOrResumeExamAttemptUseCase,
) {
    suspend operator fun invoke(slotId: String): ApiResponse<ScheduledExamStart> {
        val slot = when (val r = routineRepository.start(slotId)) {
            is ApiResponse.Success -> r.data
            is ApiResponse.Failure -> return r
        }
        return when (val attempt = startOrResumeAttempt(slot.examId)) {
            is ApiResponse.Success -> ApiResponse.Success(ScheduledExamStart(slot, attempt.data), attempt.message)
            is ApiResponse.Failure -> attempt
        }
    }
}
