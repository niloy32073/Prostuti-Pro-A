package com.prostuti.student.ui.screen.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.GetAttemptAnalysisUseCase
import com.prostuti.student.domain.TopicAnalysisRow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttemptAnalysisUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Server order: weakest (most wrong) topic first. */
    val topics: List<TopicAnalysisRow> = emptyList(),
    val errors: Map<String, String> = emptyMap()
) {
    /** The first topic with at least one wrong answer, shown in the "weakest topic" card. */
    val weakest: TopicAnalysisRow? get() = topics.firstOrNull { it.wrong > 0 }
    val totalQuestions: Int get() = topics.sumOf { it.total }
}

class AttemptAnalysisViewModel(
    private val attemptId: String,
    private val getAttemptAnalysisUseCase: GetAttemptAnalysisUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(AttemptAnalysisUiState())
    val state: StateFlow<AttemptAnalysisUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load(isRefresh = false) }

    fun refresh() = load(isRefresh = true)

    private fun load(isRefresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = !isRefresh && it.topics.isEmpty(), isRefreshing = isRefresh) }
            when (val r = getAttemptAnalysisUseCase(attemptId)) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, isRefreshing = false, topics = r.data) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, isRefreshing = false, errors = it.errors + (ERR_LOAD to r.message))
                }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }

    private companion object {
        const val ERR_LOAD = "LOAD"
    }
}
