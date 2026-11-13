package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.request.GroupUpdateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.domain.group.DeleteGroupUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.group.LeaveGroupUseCase
import com.prostuti.student.domain.group.RegenerateGroupCodeUseCase
import com.prostuti.student.domain.group.UpdateGroupUseCase
import com.prostuti.student.ui.screen.group.component.GroupNotice
import com.prostuti.student.util.ApiErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The edit form, kept as text so a half-typed number survives recomposition. */
data class GroupSettingsForm(
    val name: String = "",
    val description: String = "",
    val isPublic: Boolean = false,
    val maxMembers: String = "",
)

enum class GroupSettingsAction { SAVE, REGENERATE, LEAVE, DELETE }

data class GroupSettingsUiState(
    val isLoading: Boolean = true,
    val group: StudyGroup? = null,
    val form: GroupSettingsForm = GroupSettingsForm(),
    val running: GroupSettingsAction? = null,
    val notice: GroupNotice? = null,
    val errors: Map<String, String> = emptyMap(),
    /** One-shot: close every screen of this group (I deleted or left it). */
    val isClosed: Boolean = false,
    /** One-shot: I'm not (or no longer) an admin — go back. */
    val isNotAdmin: Boolean = false,
) {
    private val maxMembersValue: Int? get() = form.maxMembers.trim().toIntOrNull()

    val nameValid: Boolean get() = form.name.trim().length in NAME_LENGTH
    /** Null = fine; otherwise why the member cap can't be saved. */
    val maxMembersProblem: MaxMembersProblem?
        get() {
            val value = maxMembersValue ?: return MaxMembersProblem.NOT_A_NUMBER
            return when {
                value !in MAX_MEMBERS_RANGE -> MaxMembersProblem.OUT_OF_RANGE
                value < (group?.memberCount ?: 0) -> MaxMembersProblem.BELOW_MEMBER_COUNT
                else -> null
            }
        }

    /** Only the fields that differ from the saved group; null when nothing changed. */
    val changes: GroupUpdateRequest?
        get() {
            val g = group ?: return null
            val name = form.name.trim().takeIf { it != g.name }
            val description = form.description.trim().takeIf { it != g.description.orEmpty() }
            val isPublic = form.isPublic.takeIf { it != g.isPublic }
            val max = maxMembersValue?.takeIf { it != g.maxMembers }
            return if (name == null && description == null && isPublic == null && max == null) null
            else GroupUpdateRequest(name = name, description = description, isPublic = isPublic, maxMembers = max)
        }

    val canSave: Boolean get() = running == null && nameValid && maxMembersProblem == null && changes != null

    /** Going from public to private auto-rejects every open join request — the screen warns about it. */
    val isTurningPrivate: Boolean get() = group?.isPublic == true && !form.isPublic

    enum class MaxMembersProblem { NOT_A_NUMBER, OUT_OF_RANGE, BELOW_MEMBER_COUNT }

    companion object {
        val NAME_LENGTH = 2..255
        val MAX_MEMBERS_RANGE = 2..5000
        const val DESCRIPTION_MAX = 1024
    }
}

/** Admin settings of one group: edit, regenerate the invite code, leave, delete. */
class GroupSettingsViewModel(
    private val groupId: String,
    private val fetchGroup: FetchGroupUseCase,
    private val updateGroup: UpdateGroupUseCase,
    private val regenerateGroupCode: RegenerateGroupCodeUseCase,
    private val leaveGroup: LeaveGroupUseCase,
    private val deleteGroup: DeleteGroupUseCase,
    private val errorMessages: ApiErrorMessages,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupSettingsUiState())
    val state: StateFlow<GroupSettingsUiState> = _state.asStateFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchGroup(groupId)) {
                is ApiResponse.Success -> _state.update {
                    it.copy(isLoading = false, group = r.data, form = r.data.toForm(), isNotAdmin = r.data.myRole != GroupRole.ADMIN)
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(
                        isLoading = false,
                        isClosed = r.errorCode == GroupRoutineErrorCode.GROUP_NOT_FOUND,
                        errors = it.errors + (ERR_LOAD to errorMessages.messageFor(r)),
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = updateForm { copy(name = value.take(GroupSettingsUiState.NAME_LENGTH.last)) }
    fun onDescriptionChange(value: String) = updateForm { copy(description = value.take(GroupSettingsUiState.DESCRIPTION_MAX)) }
    fun onPublicChange(value: Boolean) = updateForm { copy(isPublic = value) }
    fun onMaxMembersChange(value: String) = updateForm { copy(maxMembers = value.filter(Char::isDigit).take(4)) }

    private fun updateForm(change: GroupSettingsForm.() -> GroupSettingsForm) = _state.update { it.copy(form = it.form.change()) }

    fun save() {
        val body = _state.value.changes ?: return
        if (!_state.value.canSave) return
        run(GroupSettingsAction.SAVE, call = { updateGroup(groupId, body) }) { saved ->
            copy(group = saved, form = saved.toForm(), notice = GroupNotice(R.string.group_settings_saved))
        }
    }

    fun regenerateCode() = run(GroupSettingsAction.REGENERATE, call = { regenerateGroupCode(groupId) }) { updated ->
        copy(group = updated, notice = GroupNotice(R.string.group_code_regenerated))
    }

    fun leave() = run(GroupSettingsAction.LEAVE, call = { leaveGroup(groupId) }) { copy(isClosed = true) }

    fun delete() = run(GroupSettingsAction.DELETE, call = { deleteGroup(groupId) }) { copy(isClosed = true) }

    private fun <T> run(
        action: GroupSettingsAction,
        call: suspend () -> ApiResponse<T>,
        onSuccess: GroupSettingsUiState.(T) -> GroupSettingsUiState,
    ) {
        if (_state.value.running != null) return
        viewModelScope.launch {
            _state.update { it.copy(running = action) }
            when (val r = call()) {
                is ApiResponse.Success -> _state.update { it.copy(running = null).onSuccess(r.data) }
                is ApiResponse.Failure -> {
                    _state.update {
                        it.copy(
                            running = null,
                            isNotAdmin = r.errorCode == GroupRoutineErrorCode.NOT_GROUP_ADMIN,
                            isClosed = r.errorCode == GroupRoutineErrorCode.GROUP_NOT_FOUND,
                            errors = it.errors + (ERR_ACTION to errorMessages.messageFor(r)),
                        )
                    }
                }
            }
        }
    }

    fun consumeNotice() = _state.update { it.copy(notice = null) }
    fun consumeClosed() = _state.update { it.copy(isClosed = false) }
    fun consumeNotAdmin() = _state.update { it.copy(isNotAdmin = false) }
    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private fun StudyGroup.toForm() = GroupSettingsForm(
        name = name, description = description.orEmpty(), isPublic = isPublic, maxMembers = maxMembers.toString(),
    )

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_ACTION = "ACTION"
    }
}
