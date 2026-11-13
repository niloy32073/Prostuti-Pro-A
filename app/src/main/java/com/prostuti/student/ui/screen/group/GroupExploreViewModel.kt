package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.domain.group.ExploreGroupsUseCase
import com.prostuti.student.domain.group.RequestToJoinGroupUseCase
import com.prostuti.student.util.ApiErrorMessages
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which button an Explore card shows (GROUP_ROUTINE_PLAN.md §6.3). */
enum class ExploreAction { OPEN, REQUESTED, FULL, REQUEST }

fun StudyGroup.exploreAction(): ExploreAction = when {
    myRole != null -> ExploreAction.OPEN
    hasPendingRequest -> ExploreAction.REQUESTED
    memberCount >= maxMembers -> ExploreAction.FULL
    else -> ExploreAction.REQUEST
}

data class GroupExploreUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val groups: List<StudyGroup> = emptyList(),
    val page: Int = 0,
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** Groups whose join request is being sent. */
    val requesting: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap(),
)

/** "খুঁজে দেখো": public groups, searched by name/description, with a join-request button per card. */
@OptIn(FlowPreview::class)
class GroupExploreViewModel(
    private val exploreGroups: ExploreGroupsUseCase,
    private val requestToJoinGroup: RequestToJoinGroupUseCase,
    private val errorMessages: ApiErrorMessages,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupExploreUiState())
    val state: StateFlow<GroupExploreUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(reset = true)
        viewModelScope.launch {
            _state.map { it.query.trim() }.distinctUntilChanged().drop(1).debounce(SEARCH_DEBOUNCE_MS).collect { load(reset = true) }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query.take(MAX_QUERY)) }

    fun refresh() = load(reset = true)

    fun loadMore() {
        val s = _state.value
        if (s.canLoadMore && !s.isLoading && !s.isLoadingMore) load(reset = false)
    }

    private fun load(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val page = if (reset) 1 else _state.value.page + 1
            _state.update { if (reset) it.copy(isLoading = true) else it.copy(isLoadingMore = true) }
            val query = _state.value.query.trim().ifEmpty { null }
            when (val r = exploreGroups(query, page, PAGE_LIMIT)) {
                is ApiResponse.Success -> _state.update {
                    val totalPages = r.metaData?.totalPages
                    it.copy(
                        isLoading = false, isLoadingMore = false, page = page,
                        groups = if (reset) r.data else (it.groups + r.data).distinctBy { g -> g.id },
                        canLoadMore = if (totalPages != null) page < totalPages else r.data.size == PAGE_LIMIT,
                        errors = it.errors - ERR_LOAD,
                    )
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, isLoadingMore = false, errors = it.errors + (ERR_LOAD to errorMessages.messageFor(r)))
                }
            }
        }
    }

    /** Optimistic: the card turns "requested" at once and goes back if the request fails. */
    fun requestToJoin(groupId: String) {
        if (groupId in _state.value.requesting) return
        _state.update { it.copy(requesting = it.requesting + groupId).updateGroup(groupId) { copy(hasPendingRequest = true) } }
        viewModelScope.launch {
            val r = requestToJoinGroup(groupId)
            _state.update { s ->
                val done = s.copy(requesting = s.requesting - groupId)
                when {
                    r is ApiResponse.Success -> done
                    r is ApiResponse.Failure && r.errorCode == GroupRoutineErrorCode.JOIN_REQUEST_EXISTS -> done
                    r is ApiResponse.Failure && r.errorCode == GroupRoutineErrorCode.ALREADY_MEMBER ->
                        done.updateGroup(groupId) { copy(hasPendingRequest = false, myRole = GroupRole.MEMBER) }
                    r is ApiResponse.Failure -> done.updateGroup(groupId) { copy(hasPendingRequest = false) }
                        .copy(errors = done.errors + (ERR_REQUEST to errorMessages.messageFor(r)))
                    else -> done
                }
            }
        }
    }

    private fun GroupExploreUiState.updateGroup(id: String, change: StudyGroup.() -> StudyGroup) =
        copy(groups = groups.map { if (it.id == id) it.change() else it })

    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_REQUEST = "REQUEST"
        const val PAGE_LIMIT = 20
        const val MAX_QUERY = 100
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
