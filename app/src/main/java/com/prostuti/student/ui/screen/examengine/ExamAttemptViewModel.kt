package com.prostuti.student.ui.screen.examengine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.AttemptMode
import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.enums.ExamAttemptErrorCode
import com.prostuti.student.data.model.enums.ReportResourceType
import com.prostuti.student.data.model.request.ExamResponseItem
import com.prostuti.student.data.model.request.ExamResponseSubmitRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.ExamResponseAckResponse
import com.prostuti.student.data.model.response.FetchPassageData
import com.prostuti.student.data.model.response.Question
import com.prostuti.student.domain.AddBookmarkUseCase
import com.prostuti.student.domain.ClearLocalAttemptElapsedUseCase
import com.prostuti.student.domain.FetchPassageUseCase
import com.prostuti.student.domain.GetAttemptQuestionsUseCase
import com.prostuti.student.domain.GetAttemptResponsesUseCase
import com.prostuti.student.domain.GetLocalAttemptElapsedUseCase
import com.prostuti.student.domain.ObserveReportedIdsUseCase
import com.prostuti.student.domain.ObserveBookmarkedIdsUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import com.prostuti.student.domain.SaveLocalAttemptElapsedUseCase
import com.prostuti.student.domain.SubmitExamAttemptUseCase
import com.prostuti.student.domain.SubmitExamResponsesUseCase
import com.prostuti.student.domain.SubmitMcqReportUseCase
import com.prostuti.student.domain.liveRemainingSeconds
import com.prostuti.student.domain.practiceElapsedMs
import com.prostuti.student.ui.screen.bookmark.BookmarkToggler
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_BOOKMARK = "BOOKMARK"
private const val ERR_REPORT = "REPORT"
private const val ERR_RESTORE = "RESTORE"
private const val ERR_RESUME = "RESUME"
private const val ERR_SUBMIT = "SUBMIT"

/** Practice progress is written to the device every this many timer ticks (seconds). */
private const val SAVE_PROGRESS_EVERY_TICKS = 5

data class ExamAttemptUiState(
    val attemptId: String = "",
    val startAt: String = "",
    /** A group exam's window end: the LIVE timer stops there, and the screen shows it in a banner. */
    val windowClosesAt: String? = null,
    /** LIVE: time runs from start_at and leaving the app submits. PRACTICE: time pauses while away. */
    val mode: AttemptMode = AttemptMode.PRACTICE,
    /** True until the questions and the saved progress have arrived (or loading failed). */
    val isLoading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val selectedAnswers: Map<String, String> = emptyMap(),
    val passages: Map<String, FetchPassageData> = emptyMap(),
    val totalDurationSeconds: Int = 0,
    val timeRemainingSeconds: Int = 0,
    val showSubmitDialog: Boolean = false,
    /** The exam time ran out: answering is locked, the attempt submits on its own and the
     *  screen shows the "time is up" dialog instead of jumping straight to the result. */
    val isTimeUp: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val favoriteMcqIds: Set<String> = emptySet(),
    /** Reported MCQs: their flag shows red and can't be tapped again. */
    val reportedMcqIds: Set<String> = emptySet(),
    val reportResultMessage: String? = null,
    val errors: Map<String, String> = emptyMap()
)

/**
 * Drives the attempt (question-answering) flow shared by every exam mode — practice, live and
 * group all finalize to the same `/exams/attempts/{id}/submit`.
 *
 * Timing differs by [AttemptMode]:
 *  - LIVE: remaining = duration − (now − start_at). No resume: leaving the app submits, and an
 *    unfinished live attempt reopened later is submitted straight away.
 *  - PRACTICE: the clock only runs while the exam is on screen. Time used is restored on resume
 *    from the larger of the server's last `response_time` and the value saved on this device.
 * In every mode, reaching zero (on screen, or already at zero when reopened) submits without
 * asking and sets [ExamAttemptUiState.isTimeUp] so the screen shows the time-up dialog.
 */
class ExamAttemptViewModel(
    private val attemptId: String,
    private val fetchPassageUseCase: FetchPassageUseCase,
    private val getAttemptQuestionsUseCase: GetAttemptQuestionsUseCase,
    private val getAttemptResponsesUseCase: GetAttemptResponsesUseCase,
    private val getLocalAttemptElapsedUseCase: GetLocalAttemptElapsedUseCase,
    private val saveLocalAttemptElapsedUseCase: SaveLocalAttemptElapsedUseCase,
    private val clearLocalAttemptElapsedUseCase: ClearLocalAttemptElapsedUseCase,
    private val submitExamResponsesUseCase: SubmitExamResponsesUseCase,
    private val submitExamAttemptUseCase: SubmitExamAttemptUseCase,
    private val submitMcqReportUseCase: SubmitMcqReportUseCase,
    observeBookmarkedIdsUseCase: ObserveBookmarkedIdsUseCase,
    addBookmarkUseCase: AddBookmarkUseCase,
    removeBookmarkUseCase: RemoveBookmarkUseCase,
    observeReportedIdsUseCase: ObserveReportedIdsUseCase,
    private val timeProvider: TimeProvider,
    /** A group exam's window end; the LIVE timer never runs past it. Null for every other exam. */
    private val windowClosesAt: String? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(ExamAttemptUiState(attemptId = attemptId, windowClosesAt = windowClosesAt))
    val state: StateFlow<ExamAttemptUiState> = _state.asStateFlow()

    private val bookmarks = BookmarkToggler(
        scope = viewModelScope,
        type = BookmarkResourceType.MCQ,
        observeBookmarkedIds = observeBookmarkedIdsUseCase,
        addBookmark = addBookmarkUseCase,
        removeBookmark = removeBookmarkUseCase,
        onChange = { ids -> _state.update { it.copy(favoriteMcqIds = ids) } },
        onError = { message -> _state.update { it.copy(errors = it.errors + (ERR_BOOKMARK to message)) } }
    )

    init {
        viewModelScope.launch {
            observeReportedIdsUseCase(ReportResourceType.MCQ).collect { ids ->
                _state.update { it.copy(reportedMcqIds = ids) }
            }
        }
    }

    /** mcqId -> response time (ms) of answers whose save call has not succeeded yet; flushed before submit. */
    private val unsyncedResponseTimes = mutableMapOf<String, Int>()

    private var ticksSinceSave = 0

    init {
        val stashed = ExamAttemptStore.consume()
        if (stashed != null) {
            // Just started (or handed over by the QB start/resume flow, which already closed any
            // leftover live attempt) — never auto-submit on open here.
            applyAttemptData(stashed)
            restoreProgress(isReopen = false)
        } else {
            resumeAttempt()
        }
    }

    private fun resumeAttempt() {
        viewModelScope.launch {
            when (val r = getAttemptQuestionsUseCase(attemptId)) {
                is ApiResponse.Success -> {
                    applyAttemptData(r.data)
                    restoreProgress(isReopen = true)
                }
                is ApiResponse.Failure -> if (r.errorCode == ExamAttemptErrorCode.ATTEMPT_ALREADY_ENDED) {
                    _state.update { it.copy(isLoading = false, isSubmitted = true) }
                } else {
                    _state.update { it.copy(isLoading = false, errors = it.errors + (ERR_RESUME to r.message)) }
                }
            }
        }
    }

    private fun applyAttemptData(data: ExamAttemptData) {
        val totalSeconds = data.duration * 60
        _state.update {
            it.copy(
                startAt = data.startAt,
                mode = data.mode,
                questions = data.questions,
                totalDurationSeconds = totalSeconds,
                timeRemainingSeconds = when (data.mode) {
                    AttemptMode.LIVE -> liveRemainingSeconds(data.startAt, data.duration, timeProvider.nowEpochMillis(), windowClosesAt)
                    AttemptMode.PRACTICE -> totalSeconds // corrected by restoreProgress()
                }
            )
        }
        prefetchPassages(data.questions)
    }

    /**
     * Pre-selects answers saved earlier and, for practice, restores the time already used. Runs
     * for the stashed payload too, because a QB start that hit ATTEMPT_ALREADY_ACTIVE hands over a
     * resumed attempt, not a fresh one. [isReopen]: the attempt was opened from an earlier session.
     */
    private fun restoreProgress(isReopen: Boolean) {
        viewModelScope.launch {
            val saved = when (val r = getAttemptResponsesUseCase(attemptId)) {
                is ApiResponse.Success -> r.data
                is ApiResponse.Failure -> {
                    if (r.errorCode == ExamAttemptErrorCode.ATTEMPT_ALREADY_ENDED) {
                        _state.update { it.copy(isLoading = false, isSubmitted = true) }
                        return@launch
                    }
                    _state.update { it.copy(errors = it.errors + (ERR_RESTORE to r.message)) }
                    emptyList()
                }
            }
            val isPractice = _state.value.mode == AttemptMode.PRACTICE
            val elapsedMs = if (isPractice) practiceElapsedMs(saved, getLocalAttemptElapsedUseCase(attemptId)) else 0L
            _state.update {
                it.copy(
                    isLoading = false,
                    // Picks made while this call was in flight win over the older saved ones.
                    selectedAnswers = saved.toSelectedAnswers() + it.selectedAnswers,
                    timeRemainingSeconds = if (isPractice) {
                        (it.totalDurationSeconds - (elapsedMs / 1000).toInt()).coerceIn(0, it.totalDurationSeconds)
                    } else {
                        it.timeRemainingSeconds
                    }
                )
            }
            val s = _state.value
            when {
                // The time ran out (live: while away; practice: used up earlier): submit what was saved.
                s.timeRemainingSeconds == 0 && !s.isSubmitted -> onTimeUp()
                // Live exams can't be rejoined: an unfinished one opened again is submitted as is.
                isReopen && s.mode == AttemptMode.LIVE -> submitExam()
            }
        }
    }

    private fun List<ExamResponseAckResponse>.toSelectedAnswers(): Map<String, String> =
        mapNotNull { saved -> saved.selectedOptionId?.let { saved.mcqId to it } }.toMap()

    /** Milliseconds of exam time used so far — also what `response_time` reports for an answer. */
    private fun elapsedMs(s: ExamAttemptUiState = _state.value): Int =
        ((s.totalDurationSeconds - s.timeRemainingSeconds) * 1000).coerceAtLeast(0)

    private fun saveLocalProgress() {
        val s = _state.value
        if (s.mode != AttemptMode.PRACTICE || s.isLoading || s.isSubmitted) return
        val elapsed = elapsedMs(s).toLong()
        viewModelScope.launch { saveLocalAttemptElapsedUseCase(attemptId, elapsed) }
    }

    /** Live only: re-reads the clock against start_at when the screen returns to the foreground
     *  (e.g. after a phone call or screen lock), since the 1-second ticks stop meanwhile.
     *  Practice time is meant to pause, so it is left alone. */
    fun resyncTimer() {
        _state.update { s ->
            if (s.mode != AttemptMode.LIVE || s.startAt.isEmpty() || s.isSubmitted) return@update s
            val remaining = liveRemainingSeconds(s.startAt, s.totalDurationSeconds / 60, timeProvider.nowEpochMillis(), windowClosesAt)
            s.copy(timeRemainingSeconds = remaining)
        }
        if (_state.value.timeRemainingSeconds == 0 && !_state.value.isLoading) onTimeUp()
    }

    /**
     * The exam screen went to the background. [leftApp] is true when the student switched away
     * (home, another app) and false for a phone call or screen lock. Leaving a live exam submits
     * it; practice just saves the time used so it can resume from there.
     */
    fun onBackgrounded(leftApp: Boolean) {
        if (leftApp && _state.value.mode == AttemptMode.LIVE && !_state.value.isLoading) {
            submitExam()
        } else {
            saveLocalProgress()
        }
    }

    private fun prefetchPassages(questions: List<Question>) {
        val passageIds = questions.mapNotNull { it.passageId }.distinct()
        passageIds.forEach { passageId ->
            viewModelScope.launch {
                when (val r = fetchPassageUseCase(passageId)) {
                    is ApiResponse.Success ->
                        _state.update { it.copy(passages = it.passages + (passageId to r.data)) }
                    is ApiResponse.Failure ->
                        _state.update { it.copy(errors = it.errors + ("PASSAGE_$passageId" to r.message)) }
                }
            }
        }
    }

    /** The first pick is final: once a question has an answer, later picks are ignored. */
    fun selectAnswer(mcqId: String, optionId: String) {
        var accepted = false
        _state.update {
            accepted = mcqId !in it.selectedAnswers && !it.isSubmitted && !it.isTimeUp
            if (accepted) it.copy(selectedAnswers = it.selectedAnswers + (mcqId to optionId)) else it
        }
        if (!accepted) return

        val elapsedMs = elapsedMs()
        unsyncedResponseTimes[mcqId] = elapsedMs
        saveLocalProgress()
        viewModelScope.launch {
            val r = submitExamResponsesUseCase(
                attemptId,
                ExamResponseSubmitRequest(
                    responses = listOf(ExamResponseItem(mcqId = mcqId, selectedOptionId = optionId, responseTimeMs = elapsedMs))
                )
            )
            // Best-effort sync — a failed save stays in unsyncedResponseTimes and is retried by submitExam().
            if (r is ApiResponse.Success) unsyncedResponseTimes -= mcqId
        }
    }

    fun tickTimer() {
        _state.update { s -> s.copy(timeRemainingSeconds = (s.timeRemainingSeconds - 1).coerceAtLeast(0)) }
        if (_state.value.timeRemainingSeconds == 0) {
            onTimeUp()
            return
        }
        if (++ticksSinceSave >= SAVE_PROGRESS_EVERY_TICKS) {
            ticksSinceSave = 0
            saveLocalProgress()
        }
    }

    /** Time is over: lock answering and submit without asking. Once [ExamAttemptUiState.isTimeUp]
     *  is set it stays set, so the screen keeps the time-up dialog until the student opens the result. */
    private fun onTimeUp() {
        if (_state.value.isSubmitted) return
        _state.update { it.copy(isTimeUp = true, showSubmitDialog = false) }
        saveLocalProgress() // practice: if the submit fails and the app closes, reopening still finds no time left
        submitExam()
    }

    fun showSubmitDialog() { _state.update { if (it.isTimeUp) it else it.copy(showSubmitDialog = true) } }
    fun dismissSubmitDialog() { _state.update { it.copy(showSubmitDialog = false) } }

    fun submitExam() {
        if (_state.value.isSubmitting || _state.value.isSubmitted) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, showSubmitDialog = false) }
            if (!flushUnsyncedAnswers()) return@launch
            when (val r = submitExamAttemptUseCase(attemptId)) {
                is ApiResponse.Success -> onSubmitted()
                is ApiResponse.Failure -> if (r.errorCode == ExamAttemptErrorCode.ATTEMPT_ALREADY_ENDED) {
                    onSubmitted()
                } else {
                    _state.update { it.copy(isSubmitting = false, errors = it.errors + (ERR_SUBMIT to r.message)) }
                }
            }
        }
    }

    private suspend fun onSubmitted() {
        _state.update { it.copy(isSubmitting = false, isSubmitted = true) }
        clearLocalAttemptElapsedUseCase(attemptId)
    }

    /**
     * The submit endpoint only scores answers the server already has, so any answer whose save
     * failed earlier is sent again in one batch first. Returns false (and shows the error) when
     * that batch fails for a reason other than the attempt/exam being closed already.
     */
    private suspend fun flushUnsyncedAnswers(): Boolean {
        val selected = _state.value.selectedAnswers
        val pending = unsyncedResponseTimes.mapNotNull { (mcqId, timeMs) ->
            selected[mcqId]?.let { ExamResponseItem(mcqId = mcqId, selectedOptionId = it, responseTimeMs = timeMs) }
        }
        if (pending.isEmpty()) return true
        return when (val r = submitExamResponsesUseCase(attemptId, ExamResponseSubmitRequest(responses = pending))) {
            is ApiResponse.Success -> {
                pending.forEach { unsyncedResponseTimes -= it.mcqId }
                true
            }
            is ApiResponse.Failure -> when (r.errorCode) {
                ExamAttemptErrorCode.ATTEMPT_ALREADY_ENDED, ExamAttemptErrorCode.EXAM_EXPIRED -> true
                else -> {
                    _state.update { it.copy(isSubmitting = false, errors = it.errors + (ERR_SUBMIT to r.message)) }
                    false
                }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }

    fun toggleFavorite(mcqId: String) = bookmarks.toggle(mcqId)

    fun submitReport(mcqId: String, reason: String, description: String) {
        if (mcqId in _state.value.reportedMcqIds) return // reports are final
        viewModelScope.launch {
            when (val r = submitMcqReportUseCase(mcqId, reason, description.ifBlank { null })) {
                is ApiResponse.Success -> _state.update {
                    it.copy(reportResultMessage = "প্রশ্নটি রিপোর্ট করা হয়েছে।")
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(errors = it.errors + (ERR_REPORT to r.message))
                }
            }
        }
    }

    fun clearReportResultMessage() { _state.update { it.copy(reportResultMessage = null) } }
}
