package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupPastExam
import com.prostuti.student.domain.FetchGroupPastExamsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupPastExamsUiState(
    val isLoading: Boolean = true,
    val pastExams: List<GroupPastExam> = emptyList(),
    val errors: Map<String, String> = emptyMap()
)

class GroupPastExamsViewModel(
    private val groupId: String,
    private val fetchGroupPastExamsUseCase: FetchGroupPastExamsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(GroupPastExamsUiState())
    val state: StateFlow<GroupPastExamsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchGroupPastExamsUseCase(groupId)) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, pastExams = r.data) }
                is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + ("LOAD" to r.message)) }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
