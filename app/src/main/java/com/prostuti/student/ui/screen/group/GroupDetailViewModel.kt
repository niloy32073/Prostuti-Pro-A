package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamRoutine
import com.prostuti.student.data.model.response.RoutineSlot
import com.prostuti.student.data.model.response.StudyGroup
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.domain.group.FetchGroupMembersUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.group.FetchJoinRequestsUseCase
import com.prostuti.student.domain.group.LeaveGroupUseCase
import com.prostuti.student.domain.routine.DeleteAdhocExamUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import com.prostuti.student.domain.routine.FetchPastSlotsUseCase
import com.prostuti.student.data.repository.GroupExamStats
import com.prostuti.student.data.repository.GroupExamStatsProvider
import com.prostuti.student.domain.routine.OpenScheduledExamUseCase
import com.prostuti.student.ui.screen.routine.slot.ExamLaunch
import com.prostuti.student.ui.screen.routine.slot.SlotAction
import com.prostuti.student.ui.screen.routine.slot.run
import com.prostuti.student.domain.routine.RescheduleSlotUseCase
import com.prostuti.student.domain.routine.TopicLabel
import com.prostuti.student.ui.screen.group.component.GroupNotice
import com.prostuti.student.util.ApiErrorMessages
import com.prostuti.student.util.DhakaTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val group: StudyGroup? = null,
    val memberPreview: List<StudyGroupMember> = emptyList(),
    /** Open join requests (admins of public groups only); 0 hides the banner. */
    val pendingRequestCount: Int = 0,
    val isLeaving: Boolean = false,
    /** The active group routine; null = none, or not known yet ([routineKnown]). */
    val routine: ExamRoutine? = null,
    val routineKnown: Boolean = false,
    /** Today's exams (Dhaka date), routine and one-off alike, each its own card. */
    val todaySlots: List<RoutineSlot> = emptyList(),
    /** The first exam after today, for "no exam today — next exam …". */
    val nextSlot: RoutineSlot? = null,
    /** The latest ended exams (newest first), for the "past exams" preview. */
    val pastSlots: List<RoutineSlot> = emptyList(),
    /** DUMMY per past slot (submissions, average) until the API has them (§14 B1). */
    val pastStats: Map<String, GroupExamStats> = emptyMap(),
    /** Topic label per slot id. */
    val labels: Map<String, TopicLabel> = emptyMap(),
    /** The slot whose detail sheet is open. */
    val openSlotId: String? = null,
    val isSlotBusy: Boolean = false,
    /** The slot whose exam is being opened. */
    val busyExamSlotId: String? = null,
    /** One-shot: open this attempt or result. */
    val examLaunch: ExamLaunch? = null,
    /** One-shot snackbar, e.g. "routine started" after the builder closes. */
    val notice: GroupNotice? = null,
    val errors: Map<String, String> = emptyMap(),
    /** One-shot: the screen closes — the group is gone (GROUP_NOT_FOUND) or I just left it. */
    val isGone: Boolean = false,
) {
    val isAdmin: Boolean get() = group?.myRole == GroupRole.ADMIN
    /** For the admin's "create routine" button: null while unknown (the builder checks again). */
    val hasActiveRoutine: Boolean? get() = if (routineKnown) routine != null else null
    val hasSchedule: Boolean get() = routine != null || todaySlots.isNotEmpty() || nextSlot != null
    val openSlot: RoutineSlot? get() = todaySlots.firstOrNull { it.id == openSlotId }
    /** A routine exam can only move inside its routine's days. */
    val routineLastDate: LocalDate? get() = routine?.let { r -> DhakaTime.parseDate(r.startDate)?.plusDays(r.durationDays - 1L) }
}

/**
 * GroupDetail (GROUP_ROUTINE_PLAN.md §6.5): header, invite code (admins), the admin's join-request
 * banner, today's exam cards (or "no exam today — next …"), the admin's create-routine and one-off
 * exam buttons, the member preview and a member's "leave". Past exams come back in P7.
 */
class GroupDetailViewModel(
    private val groupId: String,
    private val fetchGroup: FetchGroupUseCase,
    private val fetchGroupMembers: FetchGroupMembersUseCase,
    private val fetchJoinRequests: FetchJoinRequestsUseCase,
    private val leaveGroup: LeaveGroupUseCase,
    private val fetchActiveRoutine: FetchActiveRoutineUseCase,
    private val fetchSlots: FetchSlotsUseCase,
    private val describeTopics: DescribeTopicGroupingUseCase,
    private val rescheduleSlot: RescheduleSlotUseCase,
    private val deleteAdhocExam: DeleteAdhocExamUseCase,
    private val openScheduledExam: OpenScheduledExamUseCase,
    private val fetchPastSlots: FetchPastSlotsUseCase,
    private val statsProvider: GroupExamStatsProvider,
    private val noticeBus: GroupNoticeBus,
    private val errorMessages: ApiErrorMessages,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupDetailUiState())
    val state: StateFlow<GroupDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load() }

    fun refresh() = load()

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { s -> s.copy(isLoading = true, notice = noticeBus.take(groupId) ?: s.notice) }
            val today = DhakaTime.dateAt(now().toEpochMilli())
            val membersCall = async { fetchGroupMembers(groupId, page = 1, limit = MEMBER_PREVIEW_COUNT) }
            val routineCall = async { fetchActiveRoutine(groupId) }
            val slotsCall = async { fetchSlots(dateFrom = today, groupId = groupId, limit = UPCOMING_LIMIT) }
            val pastCall = async { fetchPastSlots(groupId, now()) }
            val group = fetchGroup(groupId)
            val members = membersCall.await()
            val routine = routineCall.await()
            val slots = slotsCall.await()
            val past = (pastCall.await() as? ApiResponse.Success)?.data?.take(PAST_PREVIEW)
            when (group) {
                is ApiResponse.Failure -> _state.update {
                    it.copy(
                        isLoading = false,
                        isGone = group.errorCode == GroupRoutineErrorCode.GROUP_NOT_FOUND,
                        errors = it.errors + (ERR_LOAD to errorMessages.messageFor(group)),
                    )
                }
                is ApiResponse.Success -> {
                    val isAdmin = group.data.myRole == GroupRole.ADMIN
                    val pending = if (isAdmin && group.data.isPublic) pendingCount() else 0
                    val upcoming = (slots as? ApiResponse.Success)?.data.orEmpty().sortedBy { it.opensAt }
                    val todaySlots = upcoming.filter { it.slotDate == today.toString() }
                    val nextSlot = upcoming.firstOrNull { it.slotDate > today.toString() }
                    val labels = describeTopics(todaySlots + listOfNotNull(nextSlot) + past.orEmpty())
                    _state.update {
                        var errors = it.errors - ERR_LOAD - ERR_MEMBERS - ERR_SLOTS
                        // Not a member (e.g. opened from a notification): those lists aren't mine to see, so no error.
                        val isMember = group.data.myRole != null
                        if (isMember && members is ApiResponse.Failure) errors = errors + (ERR_MEMBERS to errorMessages.messageFor(members))
                        if (isMember && slots is ApiResponse.Failure) errors = errors + (ERR_SLOTS to errorMessages.messageFor(slots))
                        it.copy(
                            isLoading = false,
                            group = group.data,
                            memberPreview = (members as? ApiResponse.Success)?.data.orEmpty(),
                            pendingRequestCount = pending,
                            routine = (routine as? ApiResponse.Success)?.data?.routine ?: it.routine.takeIf { routine is ApiResponse.Failure },
                            routineKnown = routine is ApiResponse.Success || it.routineKnown,
                            todaySlots = if (slots is ApiResponse.Success) todaySlots else it.todaySlots,
                            nextSlot = if (slots is ApiResponse.Success) nextSlot else it.nextSlot,
                            pastSlots = past ?: it.pastSlots,
                            pastStats = past?.mapNotNull { s -> s.examId?.let { id -> s.id to statsProvider.statsFor(id, 0, group.data.memberCount) } }?.toMap() ?: it.pastStats,
                            labels = it.labels + labels,
                            errors = errors,
                        )
                    }
                }
            }
        }
    }

    /** Pending requests from one 1-item page's `total_items`; a failure just hides the banner. */
    private suspend fun pendingCount(): Int = when (val r = fetchJoinRequests(groupId, page = 1, limit = 1)) {
        is ApiResponse.Success -> r.metaData?.totalItems ?: r.data.size
        is ApiResponse.Failure -> 0
    }

    // ── Slot sheet ──

    fun openSlot(slotId: String) = _state.update { it.copy(openSlotId = slotId) }
    fun dismissSlot() = _state.update { it.copy(openSlotId = null) }

    fun reschedule(date: LocalDate) = runSlotAction { slot ->
        when (val r = rescheduleSlot(slot.id, date)) {
            is ApiResponse.Success -> GroupNotice(R.string.slot_rescheduled) to null
            is ApiResponse.Failure -> null to r
        }
    }

    fun deleteSlot() = runSlotAction { slot ->
        when (val r = deleteAdhocExam(slot.id)) {
            is ApiResponse.Success -> GroupNotice(R.string.slot_deleted) to null
            is ApiResponse.Failure -> null to r
        }
    }

    /** Runs a move/delete on the open slot; either way the list reloads, since a failure usually means it changed. */
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
                    errors = if (failure != null) it.errors + (ERR_SLOT_ACTION to errorMessages.messageFor(failure)) else it.errors,
                )
            }
            load()
        }
    }

    // ── Exam buttons ──

    /** A slot card's exam button: start/finish/practise the exam, or open my (possibly locked) result. */
    fun openExam(slotId: String, action: SlotAction) {
        val slot = _state.value.todaySlots.firstOrNull { it.id == slotId } ?: return
        if (_state.value.busyExamSlotId != null) return
        // Set before launching, so a quick second tap can't start a second exam.
        _state.update { it.copy(busyExamSlotId = slotId) }
        viewModelScope.launch {
            when (val r = openScheduledExam.run(slot, action, now())) {
                is ApiResponse.Success -> _state.update { it.copy(busyExamSlotId = null, examLaunch = ExamLaunch(slotId, r.data)) }
                is ApiResponse.Failure -> {
                    _state.update { it.copy(busyExamSlotId = null, errors = it.errors + (ERR_EXAM to errorMessages.messageFor(r))) }
                    // e.g. SLOT_NOT_OPEN: the card was out of date.
                    load()
                }
            }
        }
    }

    fun consumeExamLaunch() = _state.update { it.copy(examLaunch = null) }

    fun leave() {
        if (_state.value.isLeaving) return
        viewModelScope.launch {
            _state.update { it.copy(isLeaving = true) }
            when (val r = leaveGroup(groupId)) {
                is ApiResponse.Success -> _state.update { it.copy(isLeaving = false, isGone = true) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLeaving = false, errors = it.errors + (ERR_LEAVE to errorMessages.messageFor(r)))
                }
            }
        }
    }

    fun consumeNotice() = _state.update { it.copy(notice = null) }
    fun consumeGone() = _state.update { it.copy(isGone = false) }
    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val ERR_LOAD = "LOAD"
        const val ERR_MEMBERS = "MEMBERS"
        const val ERR_SLOTS = "SLOTS"
        const val ERR_SLOT_ACTION = "SLOT_ACTION"
        const val ERR_LEAVE = "LEAVE"
        const val ERR_EXAM = "EXAM"
        const val MEMBER_PREVIEW_COUNT = 4
        /** Today's exams plus enough after them to find the next one. */
        const val UPCOMING_LIMIT = 20
        const val PAST_PREVIEW = 3
    }
}
