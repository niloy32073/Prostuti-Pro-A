package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.enums.TimedExamState
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MyTimedExamSubmission
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.data.model.response.RankRow
import com.prostuti.student.data.model.response.TimedExamAnswerSheet
import com.prostuti.student.data.model.response.WindowRanking
import com.prostuti.student.data.repository.TimedExamRepository
import com.prostuti.student.data.service.local.ExamDao
import com.prostuti.student.data.service.local.TimedExamAttemptEntity
import com.prostuti.student.domain.TimedExamStateDeriver
import com.prostuti.student.util.TimeProvider
import com.prostuti.student.util.toBengaliDigits

private data class ScoreResult(val correct: Int, val wrong: Int, val unanswered: Int, val marks: Float)

class FakeTimedExamRepository(
    private val dao: ExamDao,
    private val time: TimeProvider
) : TimedExamRepository {

    override suspend fun questions(): List<Question> = ExamDummyData.questions

    override suspend fun examDurationMinutes(examId: String): Int {
        if (examId.startsWith("grp_")) {
            val groupId = examId.removePrefix("grp_")
            ExamDummyData.staticRoutineByGroupId[groupId]?.let { return it.examDurationMinutes }
            dao.getRoutine(groupId)?.let { return it.examDurationMinutes }
        } else {
            ExamDummyData.liveExams.find { it.examId == examId }?.let { return it.examDurationMinutes }
        }
        return 30
    }

    override suspend fun resolveWindow(examId: String): Pair<Long, Long> {
        dao.getDemoWindow(examId)?.let { return it.windowStartEpochMillis to it.windowEndEpochMillis }
        val baseline = ExamDummyData.windowBaselineByExamId[examId]
        val now = time.nowEpochMillis()
        val (start, end) = if (baseline != null) {
            now + baseline.startOffsetMillis to now + baseline.endOffsetMillis
        } else {
            // Unknown exam with no registered window yet — defensive fallback, never expected
            // once createRoutine always calls registerWindow() up front.
            now to now
        }
        registerWindow(examId, start, end)
        return start to end
    }

    override suspend fun registerWindow(examId: String, windowStartEpochMillis: Long, windowEndEpochMillis: Long) {
        dao.upsertDemoWindow(
            com.prostuti.student.data.service.local.DemoExamWindowEntity(examId, windowStartEpochMillis, windowEndEpochMillis)
        )
    }

    override suspend fun mySubmission(examId: String): MyTimedExamSubmission? {
        val row = dao.getAttempt(examId) ?: return null
        val submittedAt = row.submittedAtEpochMillis ?: return null
        return MyTimedExamSubmission(submittedAt, row.timeTakenSeconds, row.autoSubmitted)
    }

    override suspend fun inProgressAnswers(examId: String): Map<String, String> =
        parseAnswers(dao.getAttempt(examId)?.answersCsv ?: "")

    override suspend fun saveAnswer(examId: String, kind: TimedExamKind, mcqId: String, optionId: String) {
        val existing = dao.getAttempt(examId)
        if (existing?.submittedAtEpochMillis != null) return // already submitted — nothing more to save
        val answers = parseAnswers(existing?.answersCsv ?: "").toMutableMap()
        answers[mcqId] = optionId
        dao.upsertAttempt(
            TimedExamAttemptEntity(
                examId = examId,
                kind = kind.name,
                answersCsv = serializeAnswers(answers),
                startedAtEpochMillis = existing?.startedAtEpochMillis ?: time.nowEpochMillis()
            )
        )
    }

    override suspend fun submit(examId: String, kind: TimedExamKind): ApiResponse<MyTimedExamSubmission> {
        val existing = dao.getAttempt(examId)
        if (existing?.submittedAtEpochMillis != null) {
            return ApiResponse.Success(MyTimedExamSubmission(existing.submittedAtEpochMillis, existing.timeTakenSeconds, existing.autoSubmitted))
        }
        val now = time.nowEpochMillis()
        val startedAt = existing?.startedAtEpochMillis ?: now
        val answers = parseAnswers(existing?.answersCsv ?: "")
        val (markPerQuestion, negativeMark) = resolveMarkingScheme(examId)
        val score = scoreAnswers(answers, markPerQuestion, negativeMark)
        val timeTakenSeconds = ((now - startedAt) / 1000).toInt().coerceAtLeast(0)
        dao.upsertAttempt(
            TimedExamAttemptEntity(
                examId = examId, kind = kind.name, answersCsv = serializeAnswers(answers),
                startedAtEpochMillis = startedAt, submittedAtEpochMillis = now,
                correctCount = score.correct, wrongCount = score.wrong, unansweredCount = score.unanswered,
                totalMarks = score.marks, timeTakenSeconds = timeTakenSeconds, autoSubmitted = false
            )
        )
        return ApiResponse.Success(MyTimedExamSubmission(now, timeTakenSeconds, autoSubmitted = false))
    }

    override suspend fun autoSubmitAbandonedAttempt(examId: String, kind: TimedExamKind): MyTimedExamSubmission? {
        val existing = dao.getAttempt(examId) ?: return null
        if (existing.submittedAtEpochMillis != null) return null // nothing abandoned — already final
        val now = time.nowEpochMillis()
        val answers = parseAnswers(existing.answersCsv)
        val (markPerQuestion, negativeMark) = resolveMarkingScheme(examId)
        val score = scoreAnswers(answers, markPerQuestion, negativeMark)
        val timeTakenSeconds = ((now - existing.startedAtEpochMillis) / 1000).toInt().coerceAtLeast(0)
        dao.upsertAttempt(
            existing.copy(
                submittedAtEpochMillis = now, correctCount = score.correct, wrongCount = score.wrong,
                unansweredCount = score.unanswered, totalMarks = score.marks,
                timeTakenSeconds = timeTakenSeconds, autoSubmitted = true
            )
        )
        return MyTimedExamSubmission(now, timeTakenSeconds, autoSubmitted = true)
    }

    override suspend fun windowRanking(examId: String): ApiResponse<WindowRanking> {
        val seed = ExamDummyData.rankingByExamId[examId]
            ?: return ApiResponse.Failure("এই এক্সামের র‍্যাংকিং পাওয়া যায়নি", "NOT_FOUND")
        val myRow = seed.myRank?.let { rank ->
            val myScoreLabel = dao.getAttempt(examId)?.takeIf { it.submittedAtEpochMillis != null }
                ?.let { "${it.correctCount.toBengaliDigits()}/২৫" }
                ?: ExamDummyData.historicalScoreLabel
            RankRow(rank = rank, name = "নিফা রহমান", initial = "নি", scoreLabel = myScoreLabel, isMe = true)
        }
        val rows = (seed.extraRows + listOfNotNull(myRow)).sortedBy { it.rank }
        return ApiResponse.Success(
            WindowRanking(
                submittedCount = seed.submittedCount, totalParticipants = seed.totalParticipants,
                averagePercent = seed.averagePercent, myRank = seed.myRank,
                histogramBars = seed.histogramBars, myHistogramBarIndex = seed.myHistogramBarIndex, rows = rows
            )
        )
    }

    override suspend fun myAnswerSheet(examId: String): ApiResponse<TimedExamAnswerSheet> {
        val attempt = dao.getAttempt(examId)?.takeIf { it.submittedAtEpochMillis != null }
        val answers = attempt?.let { parseAnswers(it.answersCsv) } ?: ExamDummyData.historicalMockAnswers
        val (markPerQuestion, negativeMark) = resolveMarkingScheme(examId)
        val score = scoreAnswers(answers, markPerQuestion, negativeMark)
        return ApiResponse.Success(
            TimedExamAnswerSheet(
                questions = ExamDummyData.questions, myAnswers = answers,
                correctAnswers = ExamDummyData.correctAnswerByMcqId, explanations = ExamDummyData.explanationByMcqId,
                correctCount = score.correct, wrongCount = score.wrong, unansweredCount = score.unanswered,
                totalMarks = score.marks
            )
        )
    }

    override suspend fun advanceDemoState(examId: String, kind: TimedExamKind) {
        val (start, end) = resolveWindow(examId)
        val now = time.nowEpochMillis()
        val submission = mySubmission(examId)
        when (TimedExamStateDeriver.derive(start, end, submission?.submittedAtEpochMillis, now)) {
            TimedExamState.UPCOMING -> registerWindow(examId, now - 1_000, end)
            TimedExamState.OPEN_NOT_SUBMITTED -> {
                val (markPerQuestion, negativeMark) = resolveMarkingScheme(examId)
                val score = scoreAnswers(ExamDummyData.historicalMockAnswers, markPerQuestion, negativeMark)
                dao.upsertAttempt(
                    TimedExamAttemptEntity(
                        examId = examId, kind = kind.name, answersCsv = serializeAnswers(ExamDummyData.historicalMockAnswers),
                        startedAtEpochMillis = now - 60_000, submittedAtEpochMillis = now,
                        correctCount = score.correct, wrongCount = score.wrong, unansweredCount = score.unanswered,
                        totalMarks = score.marks, timeTakenSeconds = 60, autoSubmitted = false
                    )
                )
            }
            TimedExamState.OPEN_SUBMITTED -> registerWindow(examId, start, now - 1_000)
            TimedExamState.CLOSED -> {
                dao.deleteAttempt(examId)
                dao.deleteDemoWindow(examId)
                // Re-resolve against a fresh baseline so the cycle can repeat; a no-op for exams
                // without a baseline (e.g. a routine the admin just created).
                resolveWindow(examId)
            }
        }
    }

    private suspend fun resolveMarkingScheme(examId: String): Pair<Float, Float> {
        if (examId.startsWith("grp_")) {
            val groupId = examId.removePrefix("grp_")
            ExamDummyData.staticRoutineByGroupId[groupId]?.let {
                return it.markPerQuestion to (if (it.negativeMarkingEnabled) it.negativeMark else 0f)
            }
            dao.getRoutine(groupId)?.let {
                return it.markPerQuestion to (if (it.negativeMarkingEnabled) it.negativeMark else 0f)
            }
        } else {
            ExamDummyData.liveExams.find { it.examId == examId }?.let {
                return it.markPerQuestion to it.negativeMark
            }
        }
        return 1f to 0.25f
    }

    private fun scoreAnswers(answers: Map<String, String>, markPerQuestion: Float, negativeMark: Float): ScoreResult {
        var correct = 0
        var wrong = 0
        var unanswered = 0
        for (q in ExamDummyData.questions) {
            val picked = answers[q.mcqId]
            val correctId = ExamDummyData.correctAnswerByMcqId[q.mcqId]
            when {
                picked == null -> unanswered++
                picked == correctId -> correct++
                else -> wrong++
            }
        }
        val marks = correct * markPerQuestion - wrong * negativeMark
        return ScoreResult(correct, wrong, unanswered, marks)
    }

    private fun parseAnswers(csv: String): Map<String, String> =
        if (csv.isBlank()) emptyMap()
        else csv.split("|").mapNotNull { pair ->
            val idx = pair.indexOf(':')
            if (idx <= 0) null else pair.substring(0, idx) to pair.substring(idx + 1)
        }.toMap()

    private fun serializeAnswers(answers: Map<String, String>): String =
        answers.entries.joinToString("|") { "${it.key}:${it.value}" }
}
