package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupMember
import com.prostuti.student.domain.FetchGroupMembersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_LOAD = "LOAD"

data class GroupMembersUiState(
    val isLoading: Boolean = true,
    val members: List<GroupMember> = emptyList(),
    val query: String = "",
    val errors: Map<String, String> = emptyMap()
) {
    val filtered: List<GroupMember>
        get() = if (query.isBlank()) members else members.filter { it.name.contains(query, ignoreCase = true) }
}

class GroupMembersViewModel(
    private val groupId: String,
    private val fetchGroupMembersUseCase: FetchGroupMembersUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(GroupMembersUiState())
    val state: StateFlow<GroupMembersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchGroupMembersUseCase(groupId)) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, members = r.data) }
                is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message)) }
            }
        }
    }

    fun setQuery(query: String) { _state.update { it.copy(query = query) } }
    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
