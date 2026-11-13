package com.prostuti.student.ui.screen.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamRoutine
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.domain.group.FetchMyGroupsUseCase
import com.prostuti.student.domain.routine.DeleteAdhocExamUseCase
import com.prostuti.student.domain.routine.DeleteRoutineUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.domain.routine.OpenScheduledExamUseCase
import com.prostuti.student.domain.routine.RescheduleSlotUseCase
import com.prostuti.student.domain.routine.ScheduledExamOutcome
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.screen.group.component.GroupNotice
import com.prostuti.student.ui.screen.routine.slot.ExamLaunch
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import com.prostuti.student.ui.screen.routine.slot.run
import com.prostuti.student.util.ApiErrorMessages
import com.prostuti.student.util.DhakaTime
import com.prostuti.student.util.fetchAllPages
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The routine tab's toggle (design: ব্যক্তিগত | গ্রুপ). */
enum class RoutineTabMode { PERSONAL, GROUP }

data class RoutineTabUiState(
    val mode: RoutineTabMode = RoutineTabMode.PERSONAL,
    val isLoading: Boolean = true,
    /** Only a pull-to-refresh gesture shows the pull indicator; other reloads stay quiet. */
    val isRefreshing: Boolean = false,
    val today: LocalDate,
    /** Monday of the shown week. */
    val weekStart: LocalDate,
    val selectedDate: LocalDate,
    // ── Personal ──
    val routine: ExamRoutine? = null,
    val routineKnown: Boolean = false,
    /** Every exam of my personal routine. */
    val routineSlots: List<RoutineSlot> = emptyList(),
    // ── Group ──
    val groups: List<StudyGroup> = emptyList(),
    val groupsKnown: Boolean = false,
    /** Null = all my groups. */
    val groupFilter: String? = null,
    /** The shown week's group exams (personal ones left out). */
    val groupSlots: List<RoutineSlot> = emptyList(),
    // ── Shared ──
    val labels: Map<String, TopicLabel> = emptyMap(),
    val openSlotId: String? = null,
    val isSlotBusy: Boolean = false,
    val busyExamSlotId: String? = null,
    val examLaunch: ExamLaunch? = null,
    val isDeletingRoutine: Boolean = false,
    val notice: GroupNotice? = null,
    val errors: Map<String, String> = emptyMap(),
) {
    val isPersonal: Boolean get() = mode == RoutineTabMode.PERSONAL
    val routineStart: LocalDate? get() = routine?.let { DhakaTime.parseDate(it.startDate) }
    val routineEnd: LocalDate? get() = routine?.let { r -> routineStart?.plusDays(r.durationDays - 1L) }
    /** Days left including today; null without a routine. */
    val daysLeft: Long? get() = routineEnd?.let { (ChronoUnit.DAYS.between(today, it) + 1).coerceAtLeast(0) }

    private val modeSlots: List<RoutineSlot> get() = if (isPersonal) routineSlots else groupSlots
    val weekSlots: List<RoutineSlot> get() = modeSlots.filter { it.slotDate >= weekStart.toString() && it.slotDate <= weekStart.plusDays(6).toString() }
    fun slotsOn(date: LocalDate): List<RoutineSlot> = modeSlots.filter { it.slotDate == date.toString() }.sortedBy { it.opensAt }
    fun nextExamAfter(date: LocalDate): RoutineSlot? = modeSlots.filter { it.slotDate > date.toString() }.minByOrNull { it.opensAt }
    fun isInRoutine(date: LocalDate): Boolean {
        val start = routineStart ?: return false
        val end = routineEnd ?: return false
        return date in start..end
    }

    val groupNames: Map<String, String> get() = groups.associate { it.id to it.name }
    val openSlot: RoutineSlot? get() = modeSlots.firstOrNull { it.id == openSlotId }
    /** My own exams I manage; a group's only as its admin. */
    fun canManage(slot: RoutineSlot): Boolean =
        slot.groupId == null || groups.firstOrNull { it.id == slot.groupId }?.myRole == GroupRole.ADMIN

    /** Personal weeks stay between the routine's first and last week (and this week); group weeks are free. */
    val canGoBack: Boolean get() = !isPersonal || routineStart?.let { weekStart > minOf(mondayOf(it), mondayOf(today)) } ?: false
    val canGoForward: Boolean get() = !isPersonal || routineEnd?.let { weekStart < maxOf(mondayOf(it), mondayOf(today)) } ?: false
}

internal fun mondayOf(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

/**
 * The Routine tab (GROUP_ROUTINE_PLAN.md §6.16). Personal: my active routine's week by week, each
 * day's exams or (D8) the next exam's topics, and "create" when there's none. Group: the week's exams
 * across my groups, filterable by group. Exams open through the same flow as the group screens; a
 * personal exam's result isn't locked (I'm the only one taking it).
 */
class RoutineTabViewModel(
    private val fetchActiveRoutine: FetchActiveRoutineUseCase,
    private val fetchSlots: FetchSlotsUseCase,
    private val fetchMyGroups: FetchMyGroupsUseCase,
    private val describeTopics: DescribeTopicGroupingUseCase,
    private val rescheduleSlot: RescheduleSlotUseCase,
    private val deleteAdhocExam: DeleteAdhocExamUseCase,
    private val deleteRoutine: DeleteRoutineUseCase,
    private val openScheduledExam: OpenScheduledExamUseCase,
    private val errorMessages: ApiErrorMessages,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state: MutableStateFlow<RoutineTabUiState>
    val state: StateFlow<RoutineTabUiState>

    private var loadJob: Job? = null
    /** False until the screen is first shown: that first showing reuses the [init] load. */
    private var shownOnce = false

    /** Modes whose kept data may be out of date (the user was away); switching to one reloads it. */
    private val staleModes = mutableSetOf<RoutineTabMode>()

    /** The week and group filter [RoutineTabUiState.groupSlots] were loaded for; null = none yet. */
    private var loadedGroupWeek: Pair<LocalDate, String?>? = null

    init {
        val today = DhakaTime.dateAt(now().toEpochMilli())
        _state = MutableStateFlow(RoutineTabUiState(today = today, weekStart = mondayOf(today), selectedDate = today))
        state = _state.asStateFlow()
        load()
    }

    fun refresh() = load()

    /**
     * The screen is shown again (back from a tab, the builder, an exam, ...). Things may have
     * changed elsewhere (a group joined, an exam added), so the shown mode reloads quietly while
     * its kept data stays on screen, and the other mode reloads when the user switches to it.
     */
    fun onReturn() {
        if (!shownOnce) {
            shownOnce = true
            return
        }
        staleModes += RoutineTabMode.entries - _state.value.mode
        load(fresh = true)
    }

    /** The user pulled the screen down. */
    fun pullToRefresh() = load(userPull = true, fresh = true)

    fun selectMode(mode: RoutineTabMode) {
        if (mode == _state.value.mode) return
        val today = _state.value.today
        _state.update { it.copy(mode = mode, weekStart = mondayOf(today), selectedDate = today, openSlotId = null) }
        // Each mode keeps what it loaded: switching back shows it again without a new call,
        // unless the user was away from the screen since (see [onReturn]).
        val s = _state.value
        val stale = mode in staleModes
        val loaded = if (s.isPersonal) s.routineKnown else s.groupsKnown && loadedGroupWeek == (s.weekStart to s.groupFilter)
        if (stale || !loaded) load(fresh = stale)
    }

    fun selectGroup(groupId: String?) {
        if (groupId == _state.value.groupFilter) return
        _state.update { it.copy(groupFilter = groupId) }
        load()
    }

    /** [fresh] also reloads my groups list, which otherwise loads once. */
    private fun load(userPull: Boolean = false, fresh: Boolean = false) {
        loadJob?.cancel()
        staleModes -= _state.value.mode
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isRefreshing = it.isRefreshing || userPull) }
            if (_state.value.isPersonal) loadPersonal() else loadGroups(fresh)
        }
    }

    private suspend fun loadPersonal() {
        when (val r = fetchActiveRoutine(null)) {
            is ApiResponse.Success -> {
                val slots = r.data?.slots.orEmpty()
                val labels = describeTopics(slots)
                _state.update {
                    it.copy(isLoading = false, isRefreshing = false, routine = r.data?.routine, routineKnown = true, routineSlots = slots,
                        labels = it.labels + labels, errors = it.errors - ERR_LOAD)
                }
            }
            is ApiResponse.Failure -> _state.update { it.copy(isLoading = false, isRefreshing = false, errors = it.errors + (ERR_LOAD to errorMessages.messageFor(r))) }
        }
    }

    private suspend fun loadGroups(fresh: Boolean) {
        val s = _state.value
        val groups = if (s.groupsKnown && !fresh) null else fetchAllPages(GROUP_PAGE) { page, limit -> fetchMyGroups(page, limit) }
        val week = fetchSlots(dateFrom = s.weekStart, dateTo = s.weekStart.plusDays(6), groupId = s.groupFilter, limit = WEEK_LIMIT)
        // The next exam after this week, for a quiet day near the week's end.
        val after = fetchSlots(dateFrom = s.weekStart.plusDays(7), groupId = s.groupFilter, limit = 1)
        val slots = ((week as? ApiResponse.Success)?.data.orEmpty() + (after as? ApiResponse.Success)?.data.orEmpty())
            .filter { it.groupId != null }.distinctBy { it.id }
        val labels = describeTopics(slots)
        if (week is ApiResponse.Success) loadedGroupWeek = s.weekStart to s.groupFilter
        _state.update {
            var errors = it.errors - ERR_LOAD
            if (week is ApiResponse.Failure) errors = errors + (ERR_LOAD to errorMessages.messageFor(week))
            if (groups is ApiResponse.Failure) errors = errors + (ERR_LOAD to errorMessages.messageFor(groups))
            it.copy(
                isLoading = false,
                isRefreshing = false,
                groups = (groups as? ApiResponse.Success)?.data ?: it.groups,
                groupsKnown = it.groupsKnown || groups is ApiResponse.Success,
                groupSlots = if (week is ApiResponse.Success) slots else it.groupSlots,
                labels = it.labels + labels,
                errors = errors,
            )
        }
    }

    fun showWeek(forward: Boolean) {
        val s = _state.value
        if (if (forward) !s.canGoForward else !s.canGoBack) return
        val monday = s.weekStart.plusWeeks(if (forward) 1 else -1)
        val selected = if (s.today in monday..monday.plusDays(6)) s.today else monday.plusDays((s.selectedDate.dayOfWeek.value - 1).toLong())
        _state.update { it.copy(weekStart = monday, selectedDate = selected) }
        // A personal routine already has every exam; group weeks load on demand.
        if (!s.isPersonal) load()
    }

    fun selectDate(date: LocalDate) = _state.update { it.copy(selectedDate = date) }

    // ── Slot sheet ──

    fun openSlot(slotId: String) = _state.update { it.copy(openSlotId = slotId) }
    fun dismissSlot() = _state.update { it.copy(openSlotId = null) }

    fun reschedule(date: LocalDate) = runSlotAction(GroupNotice(R.string.slot_rescheduled)) { rescheduleSlot(it.id, date) }
    fun deleteSlot() = runSlotAction(GroupNotice(R.string.slot_deleted)) { deleteAdhocExam(it.id) }

    private fun runSlotAction(onSuccess: GroupNotice, action: suspend (RoutineSlot) -> ApiResponse<*>) {
        val slot = _state.value.openSlot ?: return
        if (_state.value.isSlotBusy) return
        _state.update { it.copy(isSlotBusy = true) }
        viewModelScope.launch {
            val r = action(slot)
            _state.update {
                if (r is ApiResponse.Failure) it.copy(isSlotBusy = false, errors = it.errors + (ERR_ACTION to errorMessages.messageFor(r)))
                else it.copy(isSlotBusy = false, openSlotId = null, notice = onSuccess)
            }
            load()
        }
    }

    // ── Exam buttons ──

    fun openExam(slotId: String, action: SlotAction) {
        val slot = _state.value.weekSlots.firstOrNull { it.id == slotId } ?: _state.value.openSlot ?: return
        if (_state.value.busyExamSlotId != null) return
        _state.update { it.copy(busyExamSlotId = slotId) }
        viewModelScope.launch {
            when (val r = openScheduledExam.run(slot, action, now())) {
                is ApiResponse.Success -> {
                    // A personal exam has no one to keep the result from.
                    val outcome = if (slot.groupId != null) r.data else when (val o = r.data) {
                        is ScheduledExamOutcome.Attempt -> o.copy(lockResultUntil = null)
                        is ScheduledExamOutcome.Result -> o.copy(lockResultUntil = null)
                    }
                    _state.update { it.copy(busyExamSlotId = null, examLaunch = ExamLaunch(slotId, outcome)) }
                }
                is ApiResponse.Failure -> {
                    _state.update { it.copy(busyExamSlotId = null, errors = it.errors + (ERR_ACTION to errorMessages.messageFor(r))) }
                    load()
                }
            }
        }
    }

    fun consumeExamLaunch() = _state.update { it.copy(examLaunch = null) }

    // ── Personal routine ──

    fun deleteRoutine() {
        val routine = _state.value.routine ?: return
        if (_state.value.isDeletingRoutine) return
        _state.update { it.copy(isDeletingRoutine = true) }
        viewModelScope.launch {
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
    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_ACTION = "ACTION"
        const val WEEK_LIMIT = 100
        const val GROUP_PAGE = 50
    }
}
