package com.prostuti.student.domain.routine

import com.prostuti.student.data.model.enums.ExamAttemptErrorCode
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.util.DhakaTime
import java.time.Instant

/** Where a slot's exam button leads. [lockResultUntil] keeps the result hidden until the window closes. */
sealed interface ScheduledExamOutcome {
    data class Attempt(val attempt: ExamAttemptData, val lockResultUntil: String?) : ScheduledExamOutcome
    data class Result(val attemptId: String, val lockResultUntil: String?) : ScheduledExamOutcome
}

/**
 * The exam buttons on a slot card (GROUP_ROUTINE_PLAN.md §6.12, D1, D9):
 * - [take]: start the exam — LIVE inside the window (result locked until it closes), PRACTICE after.
 *   An unfinished LIVE attempt is submitted by the engine (D9); then my result opens instead, locked.
 * - [viewResult]: my live attempt's result, locked while the window is still open.
 * The lock is only on the app side (§14 B5).
 */
class OpenScheduledExamUseCase(
    private val startScheduledExam: StartScheduledExamUseCase,
    private val findMyAttempt: FindMyAttemptForExamUseCase,
) {
    suspend fun take(slot: RoutineSlot, now: Instant): ApiResponse<ScheduledExamOutcome> =
        when (val r = startScheduledExam(slot.id)) {
            is ApiResponse.Success -> ApiResponse.Success(
                ScheduledExamOutcome.Attempt(r.data.attempt, r.data.slot.closesAt.takeIf { r.data.slot.isLive && isBefore(now, it) })
            )
            is ApiResponse.Failure ->
                if (r.errorCode == ExamAttemptErrorCode.LIVE_ATTEMPT_SUBMITTED) viewResult(slot, now, otherwise = r) else r
        }

    suspend fun viewResult(slot: RoutineSlot, now: Instant): ApiResponse<ScheduledExamOutcome> = viewResult(slot, now, otherwise = null)

    private suspend fun viewResult(slot: RoutineSlot, now: Instant, otherwise: ApiResponse.Failure?): ApiResponse<ScheduledExamOutcome> {
        val examId = slot.examId ?: return otherwise ?: noAttempt()
        return when (val r = findMyAttempt(examId, liveOnly = true)) {
            is ApiResponse.Success -> {
                val attempt = r.data ?: return otherwise ?: noAttempt()
                ApiResponse.Success(ScheduledExamOutcome.Result(attempt.id, slot.closesAt.takeIf { isBefore(now, it) }))
            }
            is ApiResponse.Failure -> r
        }
    }

    private fun isBefore(now: Instant, closesAt: String): Boolean = DhakaTime.parseInstant(closesAt)?.let { now.isBefore(it) } ?: false

    private fun noAttempt() = ApiResponse.Failure("No submitted attempt for this exam", GroupRoutineErrorCode.NO_ATTEMPT)
}
