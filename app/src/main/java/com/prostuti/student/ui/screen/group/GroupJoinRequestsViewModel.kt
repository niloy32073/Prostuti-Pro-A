package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.enums.JoinRequestStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupJoinRequest
import com.prostuti.student.domain.group.ApproveJoinRequestUseCase
import com.prostuti.student.domain.group.FetchJoinRequestsUseCase
import com.prostuti.student.domain.group.RejectJoinRequestUseCase
import com.prostuti.student.ui.screen.group.component.GroupNotice
import com.prostuti.student.util.ApiErrorMessages
import com.prostuti.student.util.fetchAllPages
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupJoinRequestsUiState(
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    val isLoading: Boolean = true,
    val requests: List<GroupJoinRequest> = emptyList(),
    /** The request a decision is running on. */
    val busyId: String? = null,
    val notice: GroupNotice? = null,
    val errors: Map<String, String> = emptyMap(),
    /** One-shot: I'm not an admin of this group (any more) — go back. */
    val isNotAdmin: Boolean = false,
)

/** An admin's join-request queue: pending (approve/reject), approved and rejected tabs, oldest first. */
class GroupJoinRequestsViewModel(
    private val groupId: String,
    private val fetchJoinRequests: FetchJoinRequestsUseCase,
    private val approveJoinRequest: ApproveJoinRequestUseCase,
    private val rejectJoinRequest: RejectJoinRequestUseCase,
    private val errorMessages: ApiErrorMessages,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupJoinRequestsUiState())
    val state: StateFlow<GroupJoinRequestsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load() }

    fun refresh() = load()

    fun selectStatus(status: JoinRequestStatus) {
        if (status == _state.value.status) return
        _state.update { it.copy(status = status, requests = emptyList()) }
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val status = _state.value.status
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchAllPages(PAGE_LIMIT, MAX_PAGES) { page, limit -> fetchJoinRequests(groupId, status, page, limit) }) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, requests = r.data, errors = it.errors - ERR_LOAD) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(
                        isLoading = false,
                        isNotAdmin = r.errorCode == GroupRoutineErrorCode.NOT_GROUP_ADMIN,
                        errors = it.errors + (ERR_LOAD to errorMessages.messageFor(r)),
                    )
                }
            }
        }
    }

    fun approve(request: GroupJoinRequest) = decide(request, GroupNotice(R.string.group_request_approved)) {
        approveJoinRequest(groupId, request.id)
    }

    fun reject(request: GroupJoinRequest) = decide(request, GroupNotice(R.string.group_request_rejected)) {
        rejectJoinRequest(groupId, request.id)
    }

    private fun decide(request: GroupJoinRequest, onSuccess: GroupNotice, call: suspend () -> ApiResponse<GroupJoinRequest>) {
        if (_state.value.busyId != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyId = request.id) }
            val r = call()
            when (r) {
                is ApiResponse.Success -> _state.update {
                    it.copy(busyId = null, requests = it.requests.filterNot { q -> q.id == request.id }, notice = onSuccess)
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(
                        busyId = null,
                        isNotAdmin = r.errorCode == GroupRoutineErrorCode.NOT_GROUP_ADMIN,
                        errors = it.errors + (ERR_ACTION to errorMessages.messageFor(r)),
                    )
                }
            }
            // Another admin may already have decided it: the list is out of date.
            if (r is ApiResponse.Failure && r.errorCode in STALE_CODES) load()
        }
    }

    fun consumeNotice() = _state.update { it.copy(notice = null) }
    fun consumeNotAdmin() = _state.update { it.copy(isNotAdmin = false) }
    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_ACTION = "ACTION"
        const val PAGE_LIMIT = 50
        const val MAX_PAGES = 10
        val STALE_CODES = setOf(GroupRoutineErrorCode.JOIN_REQUEST_DECIDED, GroupRoutineErrorCode.JOIN_REQUEST_NOT_FOUND)
    }
}
