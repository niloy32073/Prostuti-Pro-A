package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.repository.GroupExamStats
import com.prostuti.student.data.repository.GroupExamStatsProvider
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.FetchPastSlotsUseCase
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.util.ApiErrorMessages
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class GroupPastExamsUiState(
    val isLoading: Boolean = true,
    val groupName: String? = null,
    val slots: List<RoutineSlot> = emptyList(),
    val labels: Map<String, TopicLabel> = emptyMap(),
    /** DUMMY per exam (submissions, average) until the API has them (§14 B1). */
    val stats: Map<String, GroupExamStats> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
)

/** A group's ended exams, newest first (GROUP_ROUTINE_PLAN.md §6.14); a row opens its ranking. */
class GroupPastExamsViewModel(
    private val groupId: String,
    private val fetchGroup: FetchGroupUseCase,
    private val fetchPastSlots: FetchPastSlotsUseCase,
    private val describeTopics: DescribeTopicGroupingUseCase,
    private val statsProvider: GroupExamStatsProvider,
    private val errorMessages: ApiErrorMessages,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupPastExamsUiState())
    val state: StateFlow<GroupPastExamsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load() }

    fun refresh() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val groupCall = async { fetchGroup(groupId) }
            val slots = fetchPastSlots(groupId, now())
            val group = (groupCall.await() as? ApiResponse.Success)?.data
            when (slots) {
                is ApiResponse.Success -> {
                    val labels = describeTopics(slots.data)
                    val members = group?.memberCount ?: 0
                    _state.update {
                        it.copy(
                            isLoading = false, groupName = group?.name ?: it.groupName, slots = slots.data, labels = labels,
                            stats = slots.data.mapNotNull { s -> s.examId?.let { id -> s.id to statsProvider.statsFor(id, 0, members) } }.toMap(),
                            errors = it.errors - ERR_LOAD,
                        )
                    }
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, groupName = group?.name ?: it.groupName, errors = it.errors + (ERR_LOAD to errorMessages.messageFor(slots)))
                }
            }
        }
    }

    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
    }
}
