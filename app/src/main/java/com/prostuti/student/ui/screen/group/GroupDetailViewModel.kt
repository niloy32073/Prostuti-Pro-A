package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.TimedExamKind
import com.prostuti.student.data.model.enums.TimedExamState
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.GroupDetail
import com.prostuti.student.domain.AdvanceDemoStateUseCase
import com.prostuti.student.domain.FetchGroupDetailUseCase
import com.prostuti.student.domain.TimedExamStateDeriver
import com.prostuti.student.util.TimeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_LOAD = "LOAD"

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val detail: GroupDetail? = null,
    val now: Long = 0L,
    val errors: Map<String, String> = emptyMap(),
    val isDemoAdvancing: Boolean = false
) {
    val examState: TimedExamState?
        get() = detail?.currentExam?.let {
            TimedExamStateDeriver.derive(it.windowStartEpochMillis, it.windowEndEpochMillis, detail.mySubmission?.submittedAtEpochMillis, now)
        }
}

class GroupDetailViewModel(
    private val groupId: String,
    private val fetchGroupDetailUseCase: FetchGroupDetailUseCase,
    private val advanceDemoStateUseCase: AdvanceDemoStateUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val _state = MutableStateFlow(GroupDetailUiState())
    val state: StateFlow<GroupDetailUiState> = _state.asStateFlow()

    init {
        load()
        startTicker()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchGroupDetailUseCase(groupId)) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, detail = r.data, now = timeProvider.nowEpochMillis()) }
                is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message)) }
            }
        }
    }

    /** Keeps the countdown/"বাকি সময়" labels honest while the screen stays open. */
    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _state.update { it.copy(now = timeProvider.nowEpochMillis()) }
            }
        }
    }

    fun advanceDemoState() {
        val examId = _state.value.detail?.currentExam?.examId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDemoAdvancing = true) }
            advanceDemoStateUseCase(examId, TimedExamKind.GROUP)
            load()
            _state.update { it.copy(isDemoAdvancing = false) }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
