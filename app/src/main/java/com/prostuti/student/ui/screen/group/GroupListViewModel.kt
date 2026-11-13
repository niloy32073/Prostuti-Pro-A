package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.request.GroupCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.domain.group.CreateGroupUseCase
import com.prostuti.student.domain.group.FetchMyGroupsUseCase
import com.prostuti.student.domain.group.JoinGroupByCodeUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.util.ApiErrorMessages
import com.prostuti.student.util.DhakaTime
import com.prostuti.student.util.fetchAllPages
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class GroupListUiState(
    val isLoading: Boolean = true,
    val groups: List<StudyGroup> = emptyList(),
    /** Today's exams by group id, for each card's badge. */
    val todaySlots: Map<String, List<RoutineSlot>> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val showCreateJoinSheet: Boolean = false,
    val isSubmittingCreateJoin: Boolean = false,
    val createJoinError: String? = null,
    /** One-shot: the screen navigates to this group right after creating/joining it, then clears it. */
    val navigateToGroupId: String? = null,
)

/**
 * "আমার গ্রুপ": the groups I belong to (`GET /groups/manage`, newest membership first) and the
 * create / join-by-code sheet. Each card shows where that group's exams stand today: all groups'
 * slots for today come in one call, matched by group id (GROUP_ROUTINE_PLAN.md §6.4).
 */
class GroupListViewModel(
    private val fetchMyGroupsUseCase: FetchMyGroupsUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val joinGroupByCodeUseCase: JoinGroupByCodeUseCase,
    private val fetchSlotsUseCase: FetchSlotsUseCase,
    private val errorMessages: ApiErrorMessages,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupListUiState())
    val state: StateFlow<GroupListUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load() }

    fun refresh() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val today = DhakaTime.dateAt(now().toEpochMilli())
            // A failure here only hides the badges.
            val slotsCall = async { fetchSlotsUseCase(dateFrom = today, dateTo = today, limit = SLOT_LIMIT) }
            when (val r = fetchAllPages(PAGE_LIMIT) { page, limit -> fetchMyGroupsUseCase(page, limit) }) {
                is ApiResponse.Success -> {
                    val slots = (slotsCall.await() as? ApiResponse.Success)?.data
                    _state.update {
                        it.copy(
                            isLoading = false, groups = r.data, errors = it.errors - ERR_LOAD,
                            todaySlots = slots?.filter { s -> s.groupId != null }?.groupBy { s -> s.groupId!! } ?: it.todaySlots,
                        )
                    }
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to errorMessages.messageFor(r)))
                }
            }
        }
    }

    fun openCreateJoinSheet() = _state.update { it.copy(showCreateJoinSheet = true, createJoinError = null) }
    fun dismissCreateJoinSheet() = _state.update { it.copy(showCreateJoinSheet = false, createJoinError = null) }

    fun createGroup(name: String, description: String, isPublic: Boolean) = submit {
        createGroupUseCase(GroupCreateRequest(name = name.trim(), description = description.trim().ifEmpty { null }, isPublic = isPublic))
    }

    fun joinGroup(code: String) = submit { joinGroupByCodeUseCase(code) }

    private fun submit(call: suspend () -> ApiResponse<StudyGroup>) {
        if (_state.value.isSubmittingCreateJoin) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingCreateJoin = true, createJoinError = null) }
            when (val r = call()) {
                is ApiResponse.Success -> {
                    _state.update { it.copy(isSubmittingCreateJoin = false, showCreateJoinSheet = false, navigateToGroupId = r.data.id) }
                    load()
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isSubmittingCreateJoin = false, createJoinError = errorMessages.messageFor(r))
                }
            }
        }
    }

    fun consumeNavigation() = _state.update { it.copy(navigateToGroupId = null) }
    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val PAGE_LIMIT = 50
        const val SLOT_LIMIT = 100
    }
}
