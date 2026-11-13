package com.prostuti.student.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.ui.screen.routine.slot.TodayBadge
import com.prostuti.student.util.DhakaTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant

/** Today's group exam for Home's card: the slot and its group's name. */
data class HomeGroupExam(val slot: RoutineSlot, val groupName: String)

data class HomeGroupExamUiState(
    /** Null hides the card. */
    val exam: HomeGroupExam? = null,
    /** Every group slot today, so the card reloads when a window opens or closes. */
    val todaySlots: List<RoutineSlot> = emptyList(),
)

/**
 * Home's "গ্রুপ এক্সাম · আজ" card (GROUP_ROUTINE_PLAN.md §6.19): today's first group exam with an open
 * window, else the next to open; hidden when there is none. Home is a dashboard, so a failure just
 * keeps whatever was shown.
 */
class HomeGroupExamViewModel(
    private val fetchSlots: FetchSlotsUseCase,
    private val fetchGroup: FetchGroupUseCase,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeGroupExamUiState())
    val state: StateFlow<HomeGroupExamUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val time = now()
            val today = DhakaTime.dateAt(time.toEpochMilli())
            val slots = when (val r = fetchSlots(dateFrom = today, dateTo = today, limit = SLOT_LIMIT)) {
                is ApiResponse.Success -> r.data.filter { it.groupId != null }
                is ApiResponse.Failure -> {
                    Timber.w("Today's group slots failed: %s", r.errorCode)
                    return@launch
                }
            }
            val slot = when (val badge = TodayBadge.of(slots, time)) {
                is TodayBadge.Open -> badge.slot
                is TodayBadge.Upcoming -> badge.slot
                else -> null
            }
            if (slot == null) {
                _state.update { it.copy(exam = null, todaySlots = slots) }
                return@launch
            }
            val name = when (val g = fetchGroup(slot.groupId!!)) {
                is ApiResponse.Success -> g.data.name
                is ApiResponse.Failure -> _state.value.exam?.takeIf { it.slot.groupId == slot.groupId }?.groupName
            }
            _state.update { it.copy(exam = name?.let { n -> HomeGroupExam(slot, n) }, todaySlots = slots) }
        }
    }

    private companion object {
        const val SLOT_LIMIT = 100
    }
}
