package com.prostuti.student.ui.screen.group

import com.prostuti.student.ui.screen.routine.DayMark
import com.prostuti.student.ui.screen.routine.dayMark
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamRoutine
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.routine.DeleteAdhocExamUseCase
import com.prostuti.student.domain.routine.DeleteRoutineUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.domain.routine.OpenScheduledExamUseCase
import com.prostuti.student.ui.screen.routine.slot.ExamLaunch
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import com.prostuti.student.ui.screen.routine.slot.run
import com.prostuti.student.domain.routine.RescheduleSlotUseCase
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.screen.group.component.GroupNotice
import com.prostuti.student.ui.screen.routine.slot.SlotPhase
import com.prostuti.student.ui.screen.routine.slot.phase
import com.prostuti.student.util.ApiErrorMessages
import com.prostuti.student.util.DhakaTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class GroupCalendarUiState(
    val isLoading: Boolean = true,
    /** Only a pull-to-refresh gesture shows the pull indicator; other reloads stay quiet. */
    val isRefreshing: Boolean = false,
    val group: StudyGroup? = null,
    val routine: ExamRoutine? = null,
    /** Every exam of the active routine, for "next exam" on a day without one. */
    val routineSlots: List<RoutineSlot> = emptyList(),
    /** The shown week's exams, routine and one-off. */
    val weekSlots: List<RoutineSlot> = emptyList(),
    val today: LocalDate,
    /** Monday of the shown week (the design's week starts on Monday). */
    val weekStart: LocalDate,
    val selectedDate: LocalDate,
    val labels: Map<String, TopicLabel> = emptyMap(),
    val openSlotId: String? = null,
    val isSlotBusy: Boolean = false,
    /** The slot whose exam is being opened. */
    val busyExamSlotId: String? = null,
    /** One-shot: open this attempt or result. */
    val examLaunch: ExamLaunch? = null,
    val isDeletingRoutine: Boolean = false,
    val notice: GroupNotice? = null,
    val errors: Map<String, String> = emptyMap(),
    /** One-shot: the group is gone or I'm no longer in it. */
    val isGone: Boolean = false,
) {
    val isAdmin: Boolean get() = group?.myRole == GroupRole.ADMIN
    val days: List<LocalDate> get() = (0L..6L).map { weekStart.plusDays(it) }
    val routineStart: LocalDate? get() = routine?.let { DhakaTime.parseDate(it.startDate) }
    val routineEnd: LocalDate? get() = routine?.let { r -> routineStart?.plusDays(r.durationDays - 1L) }
    val selectedSlots: List<RoutineSlot> get() = slotsOn(selectedDate)
    val openSlot: RoutineSlot? get() = weekSlots.firstOrNull { it.id == openSlotId }
    val isEmpty: Boolean get() = !isLoading && routine == null && weekSlots.isEmpty()

    fun slotsOn(date: LocalDate): List<RoutineSlot> = weekSlots.filter { it.slotDate == date.toString() }.sortedBy { it.opensAt }

    fun isInRoutine(date: LocalDate): Boolean {
        val start = routineStart ?: return false
        val end = routineEnd ?: return false
        return date in start..end
    }

    /** The first exam after [date] that I know of (D8: a day without an exam points to it). */
    fun nextExamAfter(date: LocalDate): RoutineSlot? =
        (routineSlots + weekSlots).filter { it.slotDate > date.toString() }.minByOrNull { it.opensAt }

    fun markOf(date: LocalDate, now: Instant): DayMark = dayMark(slotsOn(date), now)

    /** With a routine, weeks stay between its first week and its last (and this week). */
    val canGoBack: Boolean get() = routineStart?.let { weekStart > minOf(mondayOf(it), mondayOf(today)) } ?: true
    val canGoForward: Boolean get() = routineEnd?.let { weekStart < maxOf(mondayOf(it), mondayOf(today)) } ?: true
}

internal fun mondayOf(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

/**
 * One group's routine calendar (GROUP_ROUTINE_PLAN.md §6.17): a Monday-first week strip, the chosen
 * day's exams (or the next exam's topics, D8), each exam's detail sheet, and the admin's "one-off
 * exam" and "delete routine". Everyone in the group sees the same calendar.
 */
class GroupRoutineCalendarViewModel(
    private val groupId: String,
    private val fetchGroup: FetchGroupUseCase,
    private val fetchActiveRoutine: FetchActiveRoutineUseCase,
    private val fetchSlots: FetchSlotsUseCase,
    private val describeTopics: DescribeTopicGroupingUseCase,
    private val rescheduleSlot: RescheduleSlotUseCase,
    private val deleteAdhocExam: DeleteAdhocExamUseCase,
    private val deleteRoutine: DeleteRoutineUseCase,
    private val openScheduledExam: OpenScheduledExamUseCase,
    private val errorMessages: ApiErrorMessages,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state: MutableStateFlow<GroupCalendarUiState>
    val state: StateFlow<GroupCalendarUiState>

    private var loadJob: Job? = null
    /** False until the screen is first shown: that first showing reuses the [init] load. */
    private var shownOnce = false
    private var weekJob: Job? = null

    init {
        val today = DhakaTime.dateAt(now().toEpochMilli())
        _state = MutableStateFlow(GroupCalendarUiState(today = today, weekStart = mondayOf(today), selectedDate = today))
        state = _state.asStateFlow()
        load()
    }

    fun refresh() = load()

    /** The screen is shown again: quietly reload, the kept data stays on screen meanwhile. */
    fun onReturn() {
        if (!shownOnce) {
            shownOnce = true
            return
        }
        refresh()
    }

    /** The user pulled the screen down. */
    fun pullToRefresh() = load(userPull = true)

    private fun load(userPull: Boolean = false) {
        loadJob?.cancel()
        weekJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isRefreshing = it.isRefreshing || userPull) }
            val routineCall = async { fetchActiveRoutine(groupId) }
            val weekCall = async { fetchWeek(_state.value.weekStart) }
            val group = fetchGroup(groupId)
            val routine = routineCall.await()
            val week = weekCall.await()
            if (group is ApiResponse.Failure) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isGone = group.errorCode == GroupRoutineErrorCode.GROUP_NOT_FOUND,
                        errors = it.errors + (ERR_LOAD to errorMessages.messageFor(group)),
                    )
                }
                return@launch
            }
            val routineData = (routine as? ApiResponse.Success)?.data
            val weekSlots = (week as? ApiResponse.Success)?.data.orEmpty()
            val selected = _state.value.selectedDate
            val next = (routineData?.slots.orEmpty() + weekSlots).filter { it.slotDate > selected.toString() }.minByOrNull { it.opensAt }
            val labels = describeTopics(weekSlots + listOfNotNull(next))
            _state.update {
                var errors = it.errors - ERR_LOAD
                if (routine is ApiResponse.Failure) errors = errors + (ERR_LOAD to errorMessages.messageFor(routine))
                if (week is ApiResponse.Failure) errors = errors + (ERR_LOAD to errorMessages.messageFor(week))
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    group = (group as ApiResponse.Success).data,
                    routine = if (routine is ApiResponse.Success) routineData?.routine else it.routine,
                    routineSlots = if (routine is ApiResponse.Success) routineData?.slots.orEmpty() else it.routineSlots,
                    weekSlots = if (week is ApiResponse.Success) weekSlots else it.weekSlots,
                    labels = it.labels + labels,
                    errors = errors,
                )
            }
        }
    }

    private suspend fun fetchWeek(monday: LocalDate) = fetchSlots(dateFrom = monday, dateTo = monday.plusDays(6), groupId = groupId, limit = WEEK_LIMIT)

    /** Another week: its exams load, and the same weekday stays chosen (today when the week holds it). */
    fun showWeek(forward: Boolean) {
        val s = _state.value
        if (if (forward) !s.canGoForward else !s.canGoBack) return
        val monday = s.weekStart.plusWeeks(if (forward) 1 else -1)
        val selected = if (s.today in monday..monday.plusDays(6)) s.today else monday.plusDays((s.selectedDate.dayOfWeek.value - 1).toLong())
        _state.update { it.copy(weekStart = monday, selectedDate = selected, weekSlots = emptyList()) }
        weekJob?.cancel()
        weekJob = viewModelScope.launch {
            when (val r = fetchWeek(monday)) {
                is ApiResponse.Success -> {
                    val next = _state.value.nextExamAfter(selected)
                    val labels = describeTopics(r.data + listOfNotNull(next))
                    _state.update { it.copy(weekSlots = r.data, labels = it.labels + labels) }
                }
                is ApiResponse.Failure -> _state.update { it.copy(errors = it.errors + (ERR_LOAD to errorMessages.messageFor(r))) }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        // The next exam after this day may be one whose label isn't known yet.
        val next = _state.value.nextExamAfter(date) ?: return
        if (next.id in _state.value.labels) return
        viewModelScope.launch {
            val labels = describeTopics(listOf(next))
            _state.update { it.copy(labels = it.labels + labels) }
        }
    }

    // ── Slot sheet ──

    fun openSlot(slotId: String) = _state.update { it.copy(openSlotId = slotId) }
    fun dismissSlot() = _state.update { it.copy(openSlotId = null) }

    fun reschedule(date: LocalDate) = runSlotAction { rescheduleSlot(it.id, date).noticeOr(GroupNotice(R.string.slot_rescheduled)) }

    fun deleteSlot() = runSlotAction { deleteAdhocExam(it.id).noticeOr(GroupNotice(R.string.slot_deleted)) }

    private fun ApiResponse<*>.noticeOr(notice: GroupNotice): Pair<GroupNotice?, ApiResponse.Failure?> =
        if (this is ApiResponse.Failure) null to this else notice to null

    private fun runSlotAction(action: suspend (RoutineSlot) -> Pair<GroupNotice?, ApiResponse.Failure?>) {
        val slot = _state.value.openSlot ?: return
        if (_state.value.isSlotBusy) return
        viewModelScope.launch {
            _state.update { it.copy(isSlotBusy = true) }
            val (notice, failure) = action(slot)
            _state.update {
                it.copy(
                    isSlotBusy = false,
                    openSlotId = if (failure == null) null else it.openSlotId,
                    notice = notice ?: it.notice,
                    errors = if (failure != null) it.errors + (ERR_ACTION to errorMessages.messageFor(failure)) else it.errors,
                )
            }
            load()
        }
    }

    // ── Exam buttons ──

    /** A slot card's exam button: start/finish/practise the exam, or open my (possibly locked) result. */
    fun openExam(slotId: String, action: SlotAction) {
        val slot = _state.value.weekSlots.firstOrNull { it.id == slotId } ?: return
        if (_state.value.busyExamSlotId != null) return
        // Set before launching, so a quick second tap can't start a second exam.
        _state.update { it.copy(busyExamSlotId = slotId) }
        viewModelScope.launch {
            when (val r = openScheduledExam.run(slot, action, now())) {
                is ApiResponse.Success -> _state.update { it.copy(busyExamSlotId = null, examLaunch = ExamLaunch(slotId, r.data)) }
                is ApiResponse.Failure -> {
                    _state.update { it.copy(busyExamSlotId = null, errors = it.errors + (ERR_ACTION to errorMessages.messageFor(r))) }
                    // e.g. SLOT_NOT_OPEN: the card was out of date.
                    load()
                }
            }
        }
    }

    fun consumeExamLaunch() = _state.update { it.copy(examLaunch = null) }

    // ── Routine ──

    fun deleteRoutine() {
        val routine = _state.value.routine ?: return
        if (_state.value.isDeletingRoutine) return
        viewModelScope.launch {
            _state.update { it.copy(isDeletingRoutine = true) }
            when (val r = deleteRoutine(routine.id)) {
                is ApiResponse.Success -> _state.update {
                    it.copy(isDeletingRoutine = false, routine = null, routineSlots = emptyList(),
                        notice = GroupNotice(R.string.calendar_routine_deleted, r.data.removedSlots.toString()))
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isDeletingRoutine = false, errors = it.errors + (ERR_ACTION to errorMessages.messageFor(r)))
                }
            }
            load()
        }
    }

    fun consumeNotice() = _state.update { it.copy(notice = null) }
    fun consumeGone() = _state.update { it.copy(isGone = false) }
    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_ACTION = "ACTION"
        const val WEEK_LIMIT = 100
    }
}
