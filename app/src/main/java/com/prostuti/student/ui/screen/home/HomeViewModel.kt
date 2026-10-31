package com.prostuti.student.ui.screen.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MeData
import com.prostuti.student.data.model.response.UnfinishedAttemptSummary
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.GetResumablePracticeAttemptUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** One of the two horizontal upcoming-exam cards at the top of Home. */
data class ExamPreviewCard(
    @StringRes val badgeRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val isPrimary: Boolean
)

data class LeaderboardDemoEntry(
    val rank: Int,
    @StringRes val nameRes: Int,
    @StringRes val initialRes: Int
)

data class HomeUiState(
    // Real data: the signed-in user from GET auth/me (null until it first loads).
    val profile: MeData? = null,
    // Every field below except the continue-attempt ones is DEMO data — there is no real
    // admission-tracker, leaderboard or upcoming-exam use case yet (backend not ready). Each is
    // kept here, clearly marked, so swapping in a real use case later is a single-field change
    // rather than a screen rewrite.
    val upcomingExams: List<ExamPreviewCard> = listOf(
        ExamPreviewCard(
            badgeRes = R.string.home_live_exam_badge_demo,
            titleRes = R.string.home_live_exam_title_demo,
            subtitleRes = R.string.home_live_exam_subtitle_demo,
            isPrimary = true
        ),
        ExamPreviewCard(
            badgeRes = R.string.home_group_exam_badge_demo,
            titleRes = R.string.home_group_exam_title_demo,
            subtitleRes = R.string.home_group_exam_subtitle_demo,
            isPrimary = false
        )
    ),
    val hasAdmissionReminder: Boolean = true,
    @StringRes val admissionTitleRes: Int = R.string.home_admission_title_demo,
    @StringRes val admissionSubtitleRes: Int = R.string.home_admission_subtitle_demo,
    val leaderboardTop3: List<LeaderboardDemoEntry> = listOf(
        LeaderboardDemoEntry(2, R.string.home_rank2_name_demo, R.string.home_rank2_initial_demo),
        LeaderboardDemoEntry(1, R.string.home_rank1_name_demo, R.string.home_rank1_initial_demo),
        LeaderboardDemoEntry(3, R.string.home_rank3_name_demo, R.string.home_rank3_initial_demo)
    ),
    val showMyRankCard: Boolean = true,
    // Real data: the newest unfinished practice attempt (null = nothing to continue).
    val continueAttempt: UnfinishedAttemptSummary? = null,
    /** Seconds left on [continueAttempt]; null when its duration is unknown. */
    val continueRemainingSeconds: Int? = null
)

class HomeViewModel(
    private val getResumablePracticeAttemptUseCase: GetResumablePracticeAttemptUseCase,
    private val getMeUseCase: GetMeUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var continueJob: Job? = null
    private var profileJob: Job? = null

    init { loadProfile() }

    /** Fetches a fresh profile once per Home visit (so a newly signed-in user never sees the
     *  previous one), then keeps following the Room cache — a refresh on the Profile screen
     *  shows up here too. */
    private fun loadProfile() {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            getMeUseCase(refresh = true).collect { r ->
                when (r) {
                    is ApiResponse.Success -> _state.update { it.copy(profile = r.data) }
                    // Dashboard: on failure keep the cached profile (if any) instead of an error.
                    is ApiResponse.Failure -> Timber.w("Profile lookup failed: %s", r.errorCode)
                }
            }
        }
    }

    /** Called every time Home comes to the foreground, so finishing or starting an exam
     *  elsewhere shows up as soon as the student is back here. */
    fun refreshContinueAttempt() {
        continueJob?.cancel()
        continueJob = viewModelScope.launch {
            when (val r = getResumablePracticeAttemptUseCase()) {
                is ApiResponse.Success -> _state.update {
                    it.copy(continueAttempt = r.data, continueRemainingSeconds = r.data?.remainingSeconds())
                }
                // Home is a dashboard: on failure keep whatever was shown last instead of an error.
                is ApiResponse.Failure -> Timber.w("Unfinished attempt lookup failed: %s", r.errorCode)
            }
        }
    }

    // Practice time is paused while the exam is closed, so this doesn't count down on Home.
    private fun UnfinishedAttemptSummary.remainingSeconds(): Int? = durationMinutes?.let { minutes ->
        val total = minutes * 60
        (total - (usedMs / 1000).toInt()).coerceIn(0, total)
    }
}
