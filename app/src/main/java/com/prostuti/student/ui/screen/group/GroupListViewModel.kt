package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.TimedExamState
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Group
import com.prostuti.student.domain.CreateGroupUseCase
import com.prostuti.student.domain.FetchGroupDetailUseCase
import com.prostuti.student.domain.FetchMyGroupsUseCase
import com.prostuti.student.domain.JoinGroupUseCase
import com.prostuti.student.domain.TimedExamStateDeriver
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GroupListBadge { NONE, NO_ROUTINE, OPEN, SUBMITTED, CLOSED }

data class GroupListRow(val group: Group, val badge: GroupListBadge, val badgeLabelBn: String)

data class GroupListUiState(
    val isLoading: Boolean = true,
    val rows: List<GroupListRow> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
    val showCreateJoinSheet: Boolean = false,
    val isSubmittingCreateJoin: Boolean = false,
    val createJoinErrorBn: String? = null,
    /** One-shot: the screen navigates to this group right after creating/joining it, then clears it. */
    val navigateToGroupId: String? = null
)

class GroupListViewModel(
    private val fetchMyGroupsUseCase: FetchMyGroupsUseCase,
    private val fetchGroupDetailUseCase: FetchGroupDetailUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val joinGroupUseCase: JoinGroupUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val _state = MutableStateFlow(GroupListUiState())
    val state: StateFlow<GroupListUiState> = _state.asStateFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            fetchMyGroupsUseCase().collect { r ->
                when (r) {
                    is ApiResponse.Success -> {
                        val rows = buildRows(r.data)
                        _state.update { it.copy(isLoading = false, rows = rows) }
                    }
                    is ApiResponse.Failure -> _state.update {
                        it.copy(isLoading = false, errors = it.errors + ("LOAD" to r.message))
                    }
                }
            }
        }
    }

    private suspend fun buildRows(groups: List<Group>): List<GroupListRow> = coroutineScope {
        groups.map { group -> async { group to fetchGroupDetailUseCase(group.id) } }.awaitAll().map { (group, result) ->
            val detail = (result as? ApiResponse.Success)?.data
            when {
                detail?.routine == null -> GroupListRow(
                    group, GroupListBadge.NO_ROUTINE,
                    if (group.myRole == GroupRole.ADMIN) "রুটিন তৈরি করো" else "কোনো চলমান এক্সাম নেই"
                )
                else -> {
                    val exam = detail.currentExam
                    val state = if (exam != null) {
                        TimedExamStateDeriver.derive(exam.windowStartEpochMillis, exam.windowEndEpochMillis, detail.mySubmission?.submittedAtEpochMillis, timeProvider.nowEpochMillis())
                    } else null
                    when (state) {
                        TimedExamState.OPEN_NOT_SUBMITTED -> GroupListRow(group, GroupListBadge.OPEN, "উইন্ডো খোলা আছে")
                        TimedExamState.UPCOMING -> GroupListRow(group, GroupListBadge.NONE, "এক্সাম আসছে")
                        TimedExamState.OPEN_SUBMITTED -> GroupListRow(group, GroupListBadge.SUBMITTED, "তুমি সাবমিট করেছো")
                        TimedExamState.CLOSED -> GroupListRow(group, GroupListBadge.CLOSED, "সম্পন্ন")
                        null -> GroupListRow(group, GroupListBadge.NONE, "কোনো চলমান এক্সাম নেই")
                    }
                }
            }
        }
    }

    fun openCreateJoinSheet() { _state.update { it.copy(showCreateJoinSheet = true, createJoinErrorBn = null) } }
    fun dismissCreateJoinSheet() { _state.update { it.copy(showCreateJoinSheet = false, createJoinErrorBn = null) } }

    fun createGroup(nameBn: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingCreateJoin = true, createJoinErrorBn = null) }
            when (val r = createGroupUseCase(nameBn)) {
                is ApiResponse.Success -> {
                    _state.update { it.copy(isSubmittingCreateJoin = false, showCreateJoinSheet = false, navigateToGroupId = r.data.id) }
                    load()
                }
                is ApiResponse.Failure -> _state.update { it.copy(isSubmittingCreateJoin = false, createJoinErrorBn = r.message) }
            }
        }
    }

    fun joinGroup(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingCreateJoin = true, createJoinErrorBn = null) }
            when (val r = joinGroupUseCase(code)) {
                is ApiResponse.Success -> {
                    _state.update { it.copy(isSubmittingCreateJoin = false, showCreateJoinSheet = false, navigateToGroupId = r.data.id) }
                    load()
                }
                is ApiResponse.Failure -> _state.update { it.copy(isSubmittingCreateJoin = false, createJoinErrorBn = r.message) }
            }
        }
    }

    fun consumeNavigation() { _state.update { it.copy(navigateToGroupId = null) } }
    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
