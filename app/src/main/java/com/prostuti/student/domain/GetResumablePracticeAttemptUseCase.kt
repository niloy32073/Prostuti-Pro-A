package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.UnfinishedAttemptSummary
import com.prostuti.student.data.repository.AttemptProgressRepository
import com.prostuti.student.data.repository.ExamRepository

/** How many unfinished attempts are looked at in one go (the API allows up to 100). */
private const val UNFINISHED_PAGE_SIZE = 20

/**
 * The student's newest unfinished PRACTICE attempt for Home's continue card, or null if none.
 *
 * Live attempts can't be resumed, so any unfinished LIVE attempt found here (left behind when the
 * app was killed before it could submit) is submitted on the spot and never returned.
 *
 * Time used is worked out the same way the exam screen does it ([practiceElapsedMs]).
 * The exam and responses lookups are best-effort: if it fails the summary is still returned
 * with those fields null, so Home can still offer "continue".
 */
class GetResumablePracticeAttemptUseCase(
    private val repository: ExamRepository,
    private val progressRepository: AttemptProgressRepository
) {
    suspend operator fun invoke(): ApiResponse<UnfinishedAttemptSummary?> {
        val list = repository.listMyAttempts(examId = null, unfinished = true, page = 1, limit = UNFINISHED_PAGE_SIZE)
        if (list !is ApiResponse.Success) return list as ApiResponse.Failure

        val (live, practice) = list.data.partition { it.mode == AttemptMode.LIVE }
        live.forEach { repository.submitAttempt(it.id) } // best-effort; retried on the next Home visit

        val latest = practice.firstOrNull() ?: return ApiResponse.Success(null) // list is newest first
        val exam = (repository.getExam(latest.examId) as? ApiResponse.Success)?.data
        val saved = (repository.getAttemptResponses(latest.id) as? ApiResponse.Success)?.data.orEmpty()
        return ApiResponse.Success(
            UnfinishedAttemptSummary(
                attemptId = latest.id,
                examId = latest.examId,
                mode = latest.mode,
                startAt = latest.startAt,
                titleBn = exam?.titleBn ?: exam?.titleEn,
                durationMinutes = exam?.duration,
                usedMs = practiceElapsedMs(saved, progressRepository.getElapsedMs(latest.id)),
                otherUnfinishedCount = practice.size - 1,
                titleEn = exam?.titleEn
            )
        )
    }
}
