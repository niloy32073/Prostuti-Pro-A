package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.data.model.response.TimedExamAnswerSheet
import com.prostuti.student.data.model.response.WindowRanking

/**
 * The attempt/result/ranking/answer-sheet operations shared by every live AND group exam — this
 * is what backs `TimedExamAttemptScreen`, `LockedResultScreen`, `WindowRankingScreen` and
 * `TimedAnswerSheetScreen` regardless of which repository (group or live) found the exam in the
 * first place. Not named in the original plan as its own repository, but splitting it out here
 * avoids duplicating this logic once in each of [GroupExamRepository] and [LiveExamRepository].
 */
interface TimedExamRepository {
    suspend fun questions(): List<Question>

    /** The fixed, per-attempt time limit (independent of the window, which may stay open much
     * longer — the window is when you're ALLOWED to start, this is how long you get once you do). */
    suspend fun examDurationMinutes(examId: String): Int

    /** Resolves (and persists, on first call) this exam's absolute window. */
    suspend fun resolveWindow(examId: String): Pair<Long, Long>

    /** Anchors a brand-new exam's window explicitly instead of from a dummy baseline offset —
     * used right after an admin creates/locks a group routine. */
    suspend fun registerWindow(examId: String, windowStartEpochMillis: Long, windowEndEpochMillis: Long)

    suspend fun mySubmission(examId: String): MyTimedExamSubmission?
    suspend fun inProgressAnswers(examId: String): Map<String, String>
    suspend fun saveAnswer(examId: String, kind: TimedExamKind, mcqId: String, optionId: String)

    suspend fun submit(examId: String, kind: TimedExamKind): ApiResponse<MyTimedExamSubmission>

    /** Called once when a timed-exam screen opens. If an unsubmitted, previously-started attempt
     * exists (the student closed the app mid-exam), it's submitted right away with whatever
     * answers were saved — the screen never actually opens in that case. Returns the submission
     * it just made, or null if there was nothing to auto-submit. */
    suspend fun autoSubmitAbandonedAttempt(examId: String, kind: TimedExamKind): MyTimedExamSubmission?

    suspend fun windowRanking(examId: String): ApiResponse<WindowRanking>
    suspend fun myAnswerSheet(examId: String): ApiResponse<TimedExamAnswerSheet>

    /** Debug-only: jumps this exam straight to its next state (see `GroupDetailScreen` /
     * `LiveExamDetailScreen`'s debug chip). */
    suspend fun advanceDemoState(examId: String, kind: TimedExamKind)
}
