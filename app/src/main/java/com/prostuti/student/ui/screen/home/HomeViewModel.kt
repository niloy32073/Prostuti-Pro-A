package com.prostuti.student.ui.screen.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.prostuti.student.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    // Every field below is DEMO data — there is no real admission-tracker, leaderboard,
    // upcoming-exam or continue-progress use case yet (backend not ready). Each is kept here,
    // clearly marked, so swapping in a real use case later is a single-field change rather than
    // a screen rewrite. See project memory for the plan once those APIs exist.
    @StringRes val greetingRes: Int = R.string.home_greeting_demo,
    @StringRes val batchStreamRes: Int = R.string.home_batch_stream_demo,
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
    val hasContinueProgress: Boolean = true,
    @StringRes val continueSubtitleRes: Int = R.string.home_continue_subtitle_demo,
    val continuePercent: Int = 48
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
}
