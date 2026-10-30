package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Group
import com.prostuti.student.data.model.response.GroupRoutineSummary
import com.prostuti.student.domain.FetchGroupDetailUseCase
import com.prostuti.student.util.toBengaliDigits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

private const val ERR_LOAD = "LOAD"

data class CalendarDay(val date: LocalDate, val weekdayLabelBn: String, val dayNumBn: String, val isExamDay: Boolean, val isToday: Boolean)

data class GroupRoutineCalendarUiState(
    val isLoading: Boolean = true,
    val group: Group? = null,
    val routine: GroupRoutineSummary? = null,
    val weekDays: List<CalendarDay> = emptyList(),
    val selectedIndex: Int = 0,
    val errors: Map<String, String> = emptyMap()
) {
    val selectedDay: CalendarDay? get() = weekDays.getOrNull(selectedIndex)
    val isAdmin: Boolean get() = group?.myRole == GroupRole.ADMIN
}

/** Not in the original design — built per the plan's "গ্রুপ রুটিন ক্যালেন্ডার" section. Shows
 * this week's 7 days; the days a routine's [GroupRoutineSummary.examWeekDaysBitmask] marks as
 * exam days, everything else is a personal-study placeholder. */
class GroupRoutineCalendarViewModel(
    private val groupId: String,
    private val fetchGroupDetailUseCase: FetchGroupDetailUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(GroupRoutineCalendarUiState())
    val state: StateFlow<GroupRoutineCalendarUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val r = fetchGroupDetailUseCase(groupId)) {
                is ApiResponse.Success -> {
                    val routine = r.data.routine
                    val today = LocalDate.now()
                    val monday = today.with(DayOfWeek.MONDAY)
                    val days = (0..6).map { i ->
                        val date = monday.plusDays(i.toLong())
                        val isExamDay = routine != null && (routine.examWeekDaysBitmask shr i) and 1 == 1
                        CalendarDay(date, weekDayLabelsBn[i], date.dayOfMonth.toBengaliDigits(), isExamDay, date == today)
                    }
                    val selected = days.indexOfFirst { it.isToday }.coerceAtLeast(0)
                    _state.update { it.copy(isLoading = false, group = r.data.group, routine = routine, weekDays = days, selectedIndex = selected) }
                }
                is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, errors = it.errors + (ERR_LOAD to r.message)) }
            }
        }
    }

    fun selectDay(index: Int) { _state.update { it.copy(selectedIndex = index) } }
    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
