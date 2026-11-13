package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.group.AddGroupMemberUseCase
import com.prostuti.student.domain.group.FetchGroupMembersUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.group.RemoveGroupMemberUseCase
import com.prostuti.student.domain.group.SetGroupMemberRoleUseCase
import com.prostuti.student.ui.screen.group.component.GroupNotice
import com.prostuti.student.util.ApiErrorMessages
import com.prostuti.student.util.fetchAllPages
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupMembersUiState(
    val isLoading: Boolean = true,
    val group: StudyGroup? = null,
    val members: List<StudyGroupMember> = emptyList(),
    /** My own user id, to mark my row "তুমি"; null until known. */
    val myUserId: String? = null,
    val query: String = "",
    /** The member an action is running on (its row shows progress, its menu is disabled). */
    val busyUserId: String? = null,
    val showAddSheet: Boolean = false,
    val isAdding: Boolean = false,
    val addError: String? = null,
    /** One-shot snackbar text. */
    val notice: GroupNotice? = null,
    val errors: Map<String, String> = emptyMap(),
) {
    val isAdmin: Boolean get() = group?.myRole == GroupRole.ADMIN
    val filtered: List<StudyGroupMember>
        get() = if (query.isBlank()) members else members.filter { it.username.contains(query.trim(), ignoreCase = true) }
}

/**
 * Every member of a group, admins first, searchable by username (the API has no search). Admins can
 * add a member by email/phone, promote/demote, and remove — the server rejects removing or demoting
 * the last admin with LAST_ADMIN.
 */
class GroupMembersViewModel(
    private val groupId: String,
    private val fetchGroup: FetchGroupUseCase,
    private val fetchGroupMembers: FetchGroupMembersUseCase,
    private val addGroupMember: AddGroupMemberUseCase,
    private val setGroupMemberRole: SetGroupMemberRoleUseCase,
    private val removeGroupMember: RemoveGroupMemberUseCase,
    private val getMe: GetMeUseCase,
    private val errorMessages: ApiErrorMessages,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupMembersUiState())
    val state: StateFlow<GroupMembersUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            // Cached profile; a failure only means no "তুমি" mark.
            val me = (getMe().firstOrNull() as? ApiResponse.Success)?.data?.userId
            _state.update { it.copy(myUserId = me) }
        }
        load()
    }

    fun refresh() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val groupCall = async { fetchGroup(groupId) }
            val members = fetchAllPages(PAGE_LIMIT) { page, limit -> fetchGroupMembers(groupId, page, limit) }
            val group = groupCall.await()
            _state.update { s ->
                var errors = s.errors - ERR_LOAD
                if (members is ApiResponse.Failure) errors = errors + (ERR_LOAD to errorMessages.messageFor(members))
                else if (group is ApiResponse.Failure) errors = errors + (ERR_LOAD to errorMessages.messageFor(group))
                s.copy(
                    isLoading = false,
                    group = (group as? ApiResponse.Success)?.data ?: s.group,
                    members = (members as? ApiResponse.Success)?.data ?: s.members,
                    errors = errors,
                )
            }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun openAddSheet() = _state.update { it.copy(showAddSheet = true, addError = null) }
    fun dismissAddSheet() = _state.update { it.copy(showAddSheet = false, addError = null) }

    fun addMember(identifier: String) {
        if (_state.value.isAdding) return
        viewModelScope.launch {
            _state.update { it.copy(isAdding = true, addError = null) }
            when (val r = addGroupMember(groupId, identifier)) {
                is ApiResponse.Success -> {
                    _state.update {
                        it.copy(isAdding = false, showAddSheet = false, notice = GroupNotice(R.string.group_member_added, r.data.username))
                    }
                    load()
                }
                is ApiResponse.Failure -> _state.update { it.copy(isAdding = false, addError = errorMessages.messageFor(r)) }
            }
        }
    }

    fun makeAdmin(member: StudyGroupMember) = runOn(member, GroupNotice(R.string.group_member_now_admin, member.username)) {
        setGroupMemberRole(groupId, member.userId, GroupRole.ADMIN)
    }

    fun removeAdmin(member: StudyGroupMember) = runOn(member, GroupNotice(R.string.group_member_now_member, member.username)) {
        setGroupMemberRole(groupId, member.userId, GroupRole.MEMBER)
    }

    fun remove(member: StudyGroupMember) = runOn(member, GroupNotice(R.string.group_member_removed, member.username)) {
        removeGroupMember(groupId, member.userId)
    }

    private fun runOn(member: StudyGroupMember, onSuccess: GroupNotice, call: suspend () -> ApiResponse<*>) {
        if (_state.value.busyUserId != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyUserId = member.userId) }
            val r = call()
            when (r) {
                is ApiResponse.Success -> _state.update { it.copy(busyUserId = null, notice = onSuccess) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(busyUserId = null, errors = it.errors + (ERR_ACTION to errorMessages.messageFor(r)))
                }
            }
            // Success changes the list; MEMBER_NOT_FOUND / NOT_GROUP_ADMIN mean it is already out of date.
            if (r !is ApiResponse.Failure || r.errorCode in STALE_CODES) load()
        }
    }

    fun consumeNotice() = _state.update { it.copy(notice = null) }
    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_ACTION = "ACTION"
        const val PAGE_LIMIT = 100
        val STALE_CODES = setOf(GroupRoutineErrorCode.MEMBER_NOT_FOUND, GroupRoutineErrorCode.NOT_GROUP_ADMIN)
    }
}
