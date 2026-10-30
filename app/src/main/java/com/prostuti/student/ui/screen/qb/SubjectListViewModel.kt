package com.prostuti.student.ui.screen.qb

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

data class SubjectListUiState(
    val isLoading: Boolean = false,
    val subjects: List<Subject> = emptyList(),
    val errors: Map<String, String> = emptyMap()
)

class SubjectListViewModel(private val fetchSubjectsUseCase: FetchSubjectsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SubjectListUiState())
    val state: StateFlow<SubjectListUiState> = _state.asStateFlow()

    init { loadSubjects() }

    fun refresh() = loadSubjects(refresh = true)

    private fun loadSubjects(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            fetchSubjectsUseCase(refresh).collect { r ->
                when (r) {
                    is ApiResponse.Success -> _state.update { it.copy(isLoading = false, subjects = r.data) }
                    is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + ("LOAD" to r.message)) }
                }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
