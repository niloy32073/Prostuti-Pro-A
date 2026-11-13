package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.LeaderboardEntry
import com.prostuti.student.data.repository.GroupExamStats
import com.prostuti.student.data.repository.GroupExamStatsProvider
import com.prostuti.student.domain.GetExamLeaderboardUseCase
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.routine.FindMyAttemptForExamUseCase
import com.prostuti.student.util.ApiErrorMessages
import com.prostuti.student.util.DhakaTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class GroupRankingUiState(
    val isLoading: Boolean = true,
    val groupName: String? = null,
    val memberCount: Int? = null,
    val entries: List<LeaderboardEntry> = emptyList(),
    val totalParticipants: Int = 0,
    val page: Int = 0,
    val isLoadingMore: Boolean = false,
    /** 404: the background job hasn't built it yet. */
    val notReady: Boolean = false,
    /** The window is still open, so there can't be a ranking yet. */
    val windowOpen: Boolean = false,
    val myUserId: String? = null,
    /** My submitted LIVE attempt; null = I'm not ranked (or not known yet). */
    val myAttemptId: String? = null,
    /** DUMMY numbers (average, spread) until the API has them — see [GroupExamStatsProvider]. */
    val stats: GroupExamStats? = null,
    val errors: Map<String, String> = emptyMap(),
) {
    val myEntry: LeaderboardEntry? get() = myUserId?.let { me -> entries.firstOrNull { it.userId == me } }
    val canLoadMore: Boolean get() = !notReady && entries.size < totalParticipants
}

/**
 * A group exam's ranking (GROUP_ROUTINE_PLAN.md §6.13, D2): the API's rank list (paged), "took part
 * X/Y", and my rank — searched up to [MY_RANK_PAGES] pages when I'm not on the first. Average score
 * and the spread are DUMMY until the API gives them (§14 B1). My row opens my answer sheet.
 */
class GroupRankingViewModel(
    private val groupId: String,
    private val examId: String,
    private val closesAt: String?,
    private val fetchGroup: FetchGroupUseCase,
    private val getLeaderboard: GetExamLeaderboardUseCase,
    private val findMyAttempt: FindMyAttemptForExamUseCase,
    private val getMe: GetMeUseCase,
    private val statsProvider: GroupExamStatsProvider,
    private val errorMessages: ApiErrorMessages,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupRankingUiState())
    val state: StateFlow<GroupRankingUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load() }

    fun refresh() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val windowOpen = closesAt?.let { DhakaTime.parseInstant(it) }?.let { now().isBefore(it) } ?: false
            _state.update { it.copy(isLoading = true, windowOpen = windowOpen) }
            val groupCall = async { fetchGroup(groupId) }
            val attemptCall = async { findMyAttempt(examId, liveOnly = true) }
            val me = (getMe().firstOrNull() as? ApiResponse.Success)?.data?.userId
            val first = getLeaderboard(examId, 1)
            val group = (groupCall.await() as? ApiResponse.Success)?.data
            val myAttempt = (attemptCall.await() as? ApiResponse.Success)?.data?.id
            _state.update { s ->
                val base = s.copy(
                    isLoading = false, groupName = group?.name ?: s.groupName, memberCount = group?.memberCount ?: s.memberCount,
                    myUserId = me ?: s.myUserId, myAttemptId = myAttempt,
                )
                when (first) {
                    is ApiResponse.Success -> base.copy(
                        entries = first.data.entries, totalParticipants = first.data.totalParticipants, page = 1, notReady = false,
                        errors = base.errors - ERR_LOAD,
                    )
                    is ApiResponse.Failure ->
                        if (first.errorCode == NOT_READY) base.copy(notReady = true, entries = emptyList(), totalParticipants = 0, page = 0)
                        else base.copy(errors = base.errors + (ERR_LOAD to errorMessages.messageFor(first)))
                }.withStats()
            }
            findMyRank()
        }
    }

    /** I'm ranked but not on the loaded pages: read on, a few pages at most. */
    private suspend fun findMyRank() {
        repeat(MY_RANK_PAGES - 1) {
            val s = _state.value
            if (s.myAttemptId == null || s.myUserId == null || s.myEntry != null || !s.canLoadMore) return
            if (!loadPage()) return
        }
    }

    fun loadMore() {
        val s = _state.value
        if (!s.canLoadMore || s.isLoading || s.isLoadingMore) return
        viewModelScope.launch { loadPage() }
    }

    private suspend fun loadPage(): Boolean {
        val next = _state.value.page + 1
        _state.update { it.copy(isLoadingMore = true) }
        return when (val r = getLeaderboard(examId, next)) {
            is ApiResponse.Success -> {
                _state.update {
                    it.copy(isLoadingMore = false, page = next, entries = (it.entries + r.data.entries).distinctBy { e -> e.userId },
                        totalParticipants = r.data.totalParticipants)
                }
                r.data.entries.isNotEmpty()
            }
            is ApiResponse.Failure -> {
                _state.update { it.copy(isLoadingMore = false, errors = it.errors + (ERR_LOAD to errorMessages.messageFor(r))) }
                false
            }
        }
    }

    private fun GroupRankingUiState.withStats() =
        copy(stats = statsProvider.statsFor(examId, totalParticipants, memberCount ?: 0))

    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        /** The leaderboard answers 404 until its background job has run. */
        const val NOT_READY = "404"
        const val MY_RANK_PAGES = 5
    }
}
