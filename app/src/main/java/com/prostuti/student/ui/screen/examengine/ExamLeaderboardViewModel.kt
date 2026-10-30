package com.prostuti.student.ui.screen.examengine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamLeaderboardData
import com.prostuti.student.domain.GetExamLeaderboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExamLeaderboardUiState(
    val isLoading: Boolean = true,
    val leaderboard: ExamLeaderboardData? = null,
    /** True once the server has 404'd — the background snapshot job hasn't computed this exam yet. */
    val notReadyYet: Boolean = false,
    val errors: Map<String, String> = emptyMap()
)

/** The leaderboard is only materialized by a background job after an exam closes — a 404 here
 *  just means "not ready yet", not a real failure, and is shown as such rather than as an error. */
class ExamLeaderboardViewModel(
    private val examId: String,
    private val getExamLeaderboardUseCase: GetExamLeaderboardUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ExamLeaderboardUiState())
    val state: StateFlow<ExamLeaderboardUiState> = _state.asStateFlow()

    init { load() }

    private fun load(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = getExamLeaderboardUseCase(examId, page)) {
                is ApiResponse.Success -> _state.update {
                    it.copy(isLoading = false, leaderboard = r.data, notReadyYet = false)
                }
                is ApiResponse.Failure -> _state.update {
                    if (r.errorCode == "404") {
                        it.copy(isLoading = false, notReadyYet = true)
                    } else {
                        it.copy(isLoading = false, errors = it.errors + ("LOAD" to r.message))
                    }
                }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
