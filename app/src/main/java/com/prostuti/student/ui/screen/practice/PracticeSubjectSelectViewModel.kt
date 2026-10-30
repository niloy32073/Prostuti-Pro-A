package com.prostuti.student.ui.screen.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.domain.FetchSubjectsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_LOAD = "LOAD"

data class PracticeSubjectSelectUiState(
    val isLoading: Boolean = false,
    val subjects: List<Subject> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap()
)

class PracticeSubjectSelectViewModel(private val fetchSubjectsUseCase: FetchSubjectsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(PracticeSubjectSelectUiState())
    val state: StateFlow<PracticeSubjectSelectUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            fetchSubjectsUseCase().collect { r ->
                when (r) {
                    is ApiResponse.Success -> _state.update { it.copy(isLoading = false, subjects = r.data) }
                    is ApiResponse.Failure -> _state.update {
                        it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message))
                    }
                }
            }
        }
    }

    fun toggleSubject(subjectId: String) {
        _state.update { s ->
            val updated = if (subjectId in s.selectedIds) s.selectedIds - subjectId else s.selectedIds + subjectId
            s.copy(selectedIds = updated)
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
