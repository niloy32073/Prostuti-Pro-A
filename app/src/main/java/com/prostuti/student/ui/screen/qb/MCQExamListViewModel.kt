package com.prostuti.student.ui.screen.qb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.ExamTypes
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamAttemptData
import com.prostuti.student.data.model.response.MCQExam
import com.prostuti.student.domain.AttemptMCQExamUseCase
import com.prostuti.student.domain.FetchMCQExamsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_ATTEMPT = "ATTEMPT"
private const val ERR_BOARD_LOAD = "BOARD_LOAD"
private const val ERR_COLLEGE_LOAD = "COLLEGE_LOAD"

enum class MCQListTab { BOARD, COLLEGE }

data class MCQExamListUiState(
    val isLoading: Boolean = false,
    val isAttempting: Boolean = false,
    val selectedTab: MCQListTab = MCQListTab.BOARD,
    val searchQuery: String = "",
    val boardExams: List<MCQExam> = emptyList(),
    val collegeExams: List<MCQExam> = emptyList(),
    val boardPage: Int = 1,
    val collegePage: Int = 1,
    val boardHasMore: Boolean = true,
    val collegeHasMore: Boolean = true,
    val selectedExam: MCQExam? = null,
    val errors: Map<String, String> = emptyMap(),
    val navigateToExam: Pair<ExamAttemptData, MCQExam>? = null
)

class MCQExamListViewModel(
    val subjectId: String,
    private val fetchMCQExamsUseCase: FetchMCQExamsUseCase,
    private val attemptMCQExamUseCase: AttemptMCQExamUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(MCQExamListUiState())
    val state: StateFlow<MCQExamListUiState> = _state.asStateFlow()

    init {
        loadBoardExams()
        loadCollegeExams()
    }

    fun selectTab(tab: MCQListTab) { _state.update { it.copy(selectedTab = tab) } }
    fun updateSearch(query: String) { _state.update { it.copy(searchQuery = query) } }
    fun selectExam(exam: MCQExam?) { _state.update { it.copy(selectedExam = exam) } }

    fun loadMoreBoard() {
        if (_state.value.isLoading || !_state.value.boardHasMore) return
        loadBoardExams(page = _state.value.boardPage + 1)
    }

    fun loadMoreCollege() {
        if (_state.value.isLoading || !_state.value.collegeHasMore) return
        loadCollegeExams(page = _state.value.collegePage + 1)
    }

    private fun loadBoardExams(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchMCQExamsUseCase(subjectId, ExamTypes.BOARD, Level.ACADEMIC, page)) {
                is ApiResponse.Success -> _state.update { s ->
                    s.copy(
                        isLoading = false,
                        boardExams = if (page == 1) r.data else s.boardExams + r.data,
                        boardPage = page,
                        boardHasMore = r.metaData?.let { page < it.totalPages } ?: false
                    )
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_BOARD_LOAD to r.message))
                }
            }
        }
    }

    private fun loadCollegeExams(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchMCQExamsUseCase(subjectId, ExamTypes.COLLEGE, Level.ACADEMIC, page)) {
                is ApiResponse.Success -> _state.update { s ->
                    s.copy(
                        isLoading = false,
                        collegeExams = if (page == 1) r.data else s.collegeExams + r.data,
                        collegePage = page,
                        collegeHasMore = r.metaData?.let { page < it.totalPages } ?: false
                    )
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_COLLEGE_LOAD to r.message))
                }
            }
        }
    }

    fun startExam(exam: MCQExam) {
        viewModelScope.launch {
            _state.update { it.copy(isAttempting = true, selectedExam = null) }
            when (val r = attemptMCQExamUseCase(exam.id)) {
                is ApiResponse.Success -> _state.update {
                    it.copy(isAttempting = false, navigateToExam = Pair(r.data, exam))
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isAttempting = false, errors = it.errors + (ERR_ATTEMPT to r.message))
                }
            }
        }
    }

    fun consumeNavigation() { _state.update { it.copy(navigateToExam = null) } }
    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
