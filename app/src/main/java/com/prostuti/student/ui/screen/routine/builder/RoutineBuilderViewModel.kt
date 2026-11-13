package com.prostuti.student.ui.screen.routine.builder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.enums.Weekday
import com.prostuti.student.data.model.request.AdhocExamRequest
import com.prostuti.student.data.model.request.RoutineCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CurriculumNames
import com.prostuti.student.data.model.response.RoutinePlan
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.domain.FetchChaptersUseCase
import com.prostuti.student.domain.FetchSubjectsUseCase
import com.prostuti.student.domain.FetchTopicsUseCase
import com.prostuti.student.domain.group.FetchGroupUseCase
import com.prostuti.student.domain.routine.CreateAdhocExamUseCase
import com.prostuti.student.domain.routine.CreateRoutineUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.PreviewRoutineUseCase
import com.prostuti.student.ui.screen.group.GroupNoticeBus
import com.prostuti.student.ui.screen.group.component.GroupNotice
import com.prostuti.student.ui.screen.practice.PracticeChapterGroup
import com.prostuti.student.ui.screen.practice.PracticeSelection
import com.prostuti.student.ui.screen.practice.PracticeSubjectGroup
import com.prostuti.student.ui.screen.practice.topicIds
import com.prostuti.student.util.ApiErrorMessages
import com.prostuti.student.util.DhakaTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

data class RoutineBuilderUiState(
    val owner: RoutineOwner,
    val mode: RoutineBuilderMode,
    val step: BuilderStep = BuilderStep.SUBJECTS,
    /** Checking that I may build this (group admin; no active routine yet). */
    val isChecking: Boolean = true,
    /** A routine is already active, so a new one can't be made until it is deleted. */
    val routineExists: Boolean = false,
    val isLoadingSubjects: Boolean = true,
    val subjects: List<Subject> = emptyList(),
    /** Step 1's picks, in the order they were ticked. */
    val selectedSubjectIds: List<String> = emptyList(),
    val isLoadingChapters: Boolean = false,
    val groups: List<PracticeSubjectGroup> = emptyList(),
    val selectedTopicIds: Set<String> = emptySet(),
    val form: RoutineBuilderForm,
    val today: LocalDate,
    val nowMinute: Int,
    val isPreviewing: Boolean = false,
    val plan: RoutinePlan? = null,
    /** The preview failed; [PreviewProblem.backTo] is the step that can fix it. */
    val previewProblem: PreviewProblem? = null,
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    /** One-shot: close the builder (saved, or I'm not this group's admin). */
    val isFinished: Boolean = false,
) {
    val isAdhoc: Boolean get() = mode == RoutineBuilderMode.ADHOC
    val stepCount: Int get() = if (isAdhoc) 3 else 4
    val showsWindow: Boolean get() = owner is RoutineOwner.Group

    val scope: RoutineScope get() = RoutineScope.from(groups, selectedTopicIds)
    val selectedChapterCount: Int
        get() = groups.sumOf { g -> g.chapters.count { c -> c.topics.any { it.id in selectedTopicIds } } }

    val formProblems: Set<RoutineBuilderForm.Problem> get() = form.problems(mode, today, nowMinute)

    val canLeaveSubjects: Boolean get() = selectedSubjectIds.size in 1..RoutineScope.MAX_SUBJECTS
    val canLeaveChapters: Boolean get() = !isLoadingChapters && groups.isNotEmpty() && scope.problem == null
    val canLeaveConfig: Boolean get() = formProblems.isEmpty() && !isSaving

    val names: CurriculumNames get() = groups.curriculumNames()
    val endDate: LocalDate? get() = DhakaTime.parseDate(form.startDate)?.plusDays(form.durationDays - 1L)
}

data class PreviewProblem(val message: String, val backTo: BuilderStep)

/** What survives process death: picks, form and step. The curriculum and preview are fetched again. */
@Serializable
private data class RoutineBuilderDraft(
    val step: BuilderStep,
    val subjectIds: List<String>,
    /** Null until step 2 has loaded (it then starts with everything ticked). */
    val topicIds: List<String>?,
    val form: RoutineBuilderForm,
)

/**
 * One builder for a personal routine, a group routine and a one-off group exam (GROUP_ROUTINE_PLAN.md
 * §6.9–6.10): subjects → chapters/topics → rules (→ preview for a routine). Everything lives here
 * instead of in routes, since a pick can hold hundreds of topic ids.
 */
class RoutineBuilderViewModel(
    owner: RoutineOwner,
    mode: RoutineBuilderMode,
    private val savedState: SavedStateHandle,
    private val fetchSubjects: FetchSubjectsUseCase,
    private val fetchChapters: FetchChaptersUseCase,
    private val fetchTopics: FetchTopicsUseCase,
    private val fetchGroup: FetchGroupUseCase,
    private val fetchActiveRoutine: FetchActiveRoutineUseCase,
    private val previewRoutine: PreviewRoutineUseCase,
    private val createRoutine: CreateRoutineUseCase,
    private val createAdhocExam: CreateAdhocExamUseCase,
    private val noticeBus: GroupNoticeBus,
    private val errorMessages: ApiErrorMessages,
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    private val adhocAllowed = mode == RoutineBuilderMode.ROUTINE || owner is RoutineOwner.Group
    private val _state = MutableStateFlow(initialState(owner, if (adhocAllowed) mode else RoutineBuilderMode.ROUTINE))
    val state: StateFlow<RoutineBuilderUiState> = _state.asStateFlow()

    private var chaptersJob: Job? = null
    private var previewJob: Job? = null
    /** Topic picks restored from saved state, applied once step 2's curriculum is back. */
    private var restoredTopicIds: Set<String>? = null

    init {
        val draft = savedState.get<String>(KEY_DRAFT)?.let { runCatching { json.decodeFromString<RoutineBuilderDraft>(it) }.getOrNull() }
        if (draft != null) {
            restoredTopicIds = draft.topicIds?.toSet()
            _state.update { it.copy(step = draft.step, selectedSubjectIds = draft.subjectIds, form = draft.form) }
        }
        check()
        loadSubjects()
        if (draft != null && draft.step != BuilderStep.SUBJECTS) loadChapters()
        if (draft?.step == BuilderStep.PREVIEW) runPreview()
        viewModelScope.launch {
            _state.map { s ->
                RoutineBuilderDraft(s.step, s.selectedSubjectIds, if (s.groups.isEmpty()) restoredTopicIds?.toList() else s.selectedTopicIds.toList(), s.form)
            }.distinctUntilChanged().collect { savedState[KEY_DRAFT] = json.encodeToString(RoutineBuilderDraft.serializer(), it) }
        }
    }

    private fun initialState(owner: RoutineOwner, mode: RoutineBuilderMode): RoutineBuilderUiState {
        val instant = now()
        val today = DhakaTime.dateAt(instant.toEpochMilli())
        return RoutineBuilderUiState(
            owner = owner, mode = mode, form = RoutineBuilderForm.initial(owner, today),
            today = today, nowMinute = DhakaTime.minuteOfDayAt(instant.toEpochMilli()),
        )
    }

    /** Group: I must be its admin. Routine: none may be active yet (else the server says ROUTINE_EXISTS). */
    private fun check() = viewModelScope.launch {
        val s = _state.value
        val groupId = s.owner.groupIdOrNull
        if (groupId != null) {
            when (val r = fetchGroup(groupId)) {
                is ApiResponse.Success -> if (r.data.myRole != GroupRole.ADMIN) {
                    _state.update { it.copy(isFinished = true) }
                    return@launch
                }
                is ApiResponse.Failure -> if (r.errorCode == GroupRoutineErrorCode.GROUP_NOT_FOUND || r.errorCode == GroupRoutineErrorCode.NOT_GROUP_ADMIN) {
                    _state.update { it.copy(isFinished = true) }
                    return@launch
                }
            }
        }
        val exists = s.mode == RoutineBuilderMode.ROUTINE &&
            (fetchActiveRoutine(groupId) as? ApiResponse.Success)?.data != null
        _state.update { it.copy(isChecking = false, routineExists = exists) }
    }

    fun refresh() {
        loadSubjects(refresh = true)
        if (_state.value.step != BuilderStep.SUBJECTS) loadChapters(refresh = true)
    }

    private fun loadSubjects(refresh: Boolean = false) = viewModelScope.launch {
        _state.update { it.copy(isLoadingSubjects = true) }
        when (val r = fetchSubjects(refresh).first()) {
            is ApiResponse.Success -> _state.update { it.copy(isLoadingSubjects = false, subjects = r.data, errors = it.errors - ERR_SUBJECTS) }
            is ApiResponse.Failure -> _state.update {
                it.copy(isLoadingSubjects = false, errors = it.errors + (ERR_SUBJECTS to errorMessages.messageFor(r)))
            }
        }
    }

    // ── Step 1 ──

    fun toggleSubject(subjectId: String) = _state.update { s ->
        when {
            subjectId in s.selectedSubjectIds -> s.copy(selectedSubjectIds = s.selectedSubjectIds - subjectId)
            s.selectedSubjectIds.size >= RoutineScope.MAX_SUBJECTS -> s
            else -> s.copy(selectedSubjectIds = s.selectedSubjectIds + subjectId)
        }
    }

    // ── Step 2 ──

    /**
     * Chapters and topics of the picked subjects. A subject already loaded keeps its picks; a newly
     * added one starts fully ticked ("পুরো বই"), the way most routines cover whole books.
     */
    private fun loadChapters(refresh: Boolean = false) {
        chaptersJob?.cancel()
        chaptersJob = viewModelScope.launch {
            val s = _state.value
            val ids = s.selectedSubjectIds
            val known = if (refresh) emptyMap() else s.groups.associateBy { it.subjectId }
            _state.update { it.copy(isLoadingChapters = true) }
            val subjects = s.subjects.associateBy { it.id }.ifEmpty {
                (fetchSubjects().first() as? ApiResponse.Success)?.data.orEmpty().associateBy { it.id }
            }
            val errors = mutableMapOf<String, String>()
            val groups = coroutineScope {
                ids.map { id -> async { known[id] ?: loadSubject(id, subjects[id]?.nameBn ?: id, subjects[id]?.nameEn.orEmpty(), refresh, errors) } }.awaitAll()
            }
            _state.update { cur ->
                val oldIds = cur.groups.flatMap { it.topicIds }.toSet()
                val allIds = groups.flatMap { it.topicIds }.toSet()
                val restored = restoredTopicIds
                val selected = when {
                    restored != null -> restored intersect allIds
                    refresh -> cur.selectedTopicIds intersect allIds
                    else -> (cur.selectedTopicIds intersect allIds) + (allIds - oldIds)
                }
                restoredTopicIds = null
                cur.copy(
                    isLoadingChapters = false, groups = groups, selectedTopicIds = selected,
                    errors = cur.errors.filterKeys { !it.startsWith(ERR_CURRICULUM) } + errors,
                )
            }
        }
    }

    private suspend fun loadSubject(subjectId: String, name: String, nameEn: String, refresh: Boolean, errors: MutableMap<String, String>): PracticeSubjectGroup = coroutineScope {
        val chapters = when (val r = fetchChapters(subjectId, refresh).first()) {
            is ApiResponse.Success -> r.data
            is ApiResponse.Failure -> {
                errors["$ERR_CURRICULUM$subjectId"] = errorMessages.messageFor(r)
                emptyList()
            }
        }
        val chapterGroups = chapters.sortedBy { it.order }.map { chapter ->
            async {
                val topics = when (val r = fetchTopics(chapter.id, refresh).first()) {
                    is ApiResponse.Success -> r.data.sortedBy { it.order }
                    is ApiResponse.Failure -> {
                        errors["$ERR_CURRICULUM${chapter.id}"] = errorMessages.messageFor(r)
                        emptyList()
                    }
                }
                PracticeChapterGroup(chapter = chapter, topics = topics)
            }
        }.awaitAll()
        PracticeSubjectGroup(subjectId = subjectId, subjectNameBn = name, chapters = chapterGroups, subjectNameEn = nameEn)
    }

    fun toggleChapterOpen(chapterId: String) = _state.update { s ->
        s.copy(groups = s.groups.map { g ->
            g.copy(chapters = g.chapters.map { c -> if (c.chapter.id == chapterId) c.copy(isExpanded = !c.isExpanded) else c })
        })
    }

    fun toggleSubjectAll(subjectId: String) = _state.update { s ->
        val group = s.groups.find { it.subjectId == subjectId } ?: return@update s
        s.copy(selectedTopicIds = PracticeSelection.toggleAll(group.topicIds, s.selectedTopicIds))
    }

    fun toggleChapterAll(chapterId: String) = _state.update { s ->
        val chapter = s.groups.flatMap { it.chapters }.find { it.chapter.id == chapterId } ?: return@update s
        s.copy(selectedTopicIds = PracticeSelection.toggleAll(chapter.topics.map { it.id }, s.selectedTopicIds))
    }

    fun toggleTopic(topicId: String) = _state.update { s -> s.copy(selectedTopicIds = PracticeSelection.toggleOne(topicId, s.selectedTopicIds)) }

    // ── Step 3 ──

    private fun updateForm(change: RoutineBuilderForm.() -> RoutineBuilderForm) = _state.update { it.copy(form = it.form.change()) }

    fun setStartDate(date: LocalDate) = updateForm { copy(startDate = date.toString()) }
    fun setExamDate(date: LocalDate) = updateForm { copy(examDate = date.toString()) }
    fun setTitle(title: String) = updateForm { copy(title = title.take(RoutineBuilderForm.MAX_TITLE + 1)) }

    fun changeDurationDays(steps: Int) {
        val range = RoutineBuilderForm.durationDaysRange(_state.value.owner)
        updateForm { copy(durationDays = (durationDays + steps * RoutineBuilderForm.DAYS_STEP).coerceIn(range)) }
    }

    fun toggleWeekday(day: Weekday) = updateForm { copy(weekdays = if (day in weekdays) weekdays - day else weekdays + day) }

    fun changeWindowOpen(steps: Int) = updateForm {
        copy(windowOpenMinute = (windowOpenMinute + steps * RoutineBuilderForm.WINDOW_STEP).coerceIn(0, DhakaTime.MINUTES_PER_DAY - RoutineBuilderForm.WINDOW_STEP))
    }

    fun changeWindowClose(steps: Int) = updateForm {
        copy(windowCloseMinute = (windowCloseMinute + steps * RoutineBuilderForm.WINDOW_STEP).coerceIn(RoutineBuilderForm.WINDOW_STEP, DhakaTime.MINUTES_PER_DAY))
    }

    fun changeQuestions(steps: Int) = updateForm {
        copy(questionsCount = (questionsCount + steps * RoutineBuilderForm.QUESTIONS_STEP).coerceIn(RoutineBuilderForm.QUESTIONS_RANGE))
    }

    fun changeDurationMinutes(steps: Int) = updateForm {
        copy(durationMinutes = (durationMinutes + steps * RoutineBuilderForm.MINUTES_STEP).coerceIn(RoutineBuilderForm.DURATION_MINUTES_RANGE))
    }

    fun changeMark(steps: Int) = updateForm {
        copy(markPerQuestion = RoutineBuilderForm.quarter(markPerQuestion + steps * RoutineBuilderForm.MARK_STEP).coerceIn(RoutineBuilderForm.MARK_RANGE))
    }

    fun setNegativeMarking(on: Boolean) = updateForm { copy(negativeMarking = on) }

    fun changeNegativeMark(steps: Int) = updateForm {
        copy(negativeMarkValue = RoutineBuilderForm.quarter(negativeMarkValue + steps * RoutineBuilderForm.MARK_STEP).coerceIn(RoutineBuilderForm.NEGATIVE_RANGE))
    }

    // ── Moving between steps ──

    fun next() {
        val s = _state.value
        when (s.step) {
            BuilderStep.SUBJECTS -> if (s.canLeaveSubjects) {
                _state.update { it.copy(step = BuilderStep.CHAPTERS) }
                if (s.groups.map { it.subjectId } != s.selectedSubjectIds) loadChapters()
            }
            BuilderStep.CHAPTERS -> if (s.canLeaveChapters) _state.update { it.copy(step = BuilderStep.CONFIG) }
            BuilderStep.CONFIG -> if (s.canLeaveConfig) {
                if (s.isAdhoc) saveAdhoc() else {
                    _state.update { it.copy(step = BuilderStep.PREVIEW) }
                    runPreview()
                }
            }
            BuilderStep.PREVIEW -> saveRoutine()
        }
    }

    /** One step back; false at step 1, where back leaves the builder. */
    fun back(): Boolean {
        val s = _state.value
        val previous = when (s.step) {
            BuilderStep.SUBJECTS -> return false
            BuilderStep.CHAPTERS -> BuilderStep.SUBJECTS
            BuilderStep.CONFIG -> BuilderStep.CHAPTERS
            BuilderStep.PREVIEW -> BuilderStep.CONFIG
        }
        if (s.step == BuilderStep.PREVIEW) previewJob?.cancel()
        _state.update { it.copy(step = previous, isPreviewing = false, previewProblem = null) }
        return true
    }

    fun goTo(step: BuilderStep) {
        if (step.ordinal >= _state.value.step.ordinal) return
        previewJob?.cancel()
        _state.update { it.copy(step = step, isPreviewing = false, previewProblem = null) }
    }

    // ── Step 4 + saving ──

    private fun routineRequest(s: RoutineBuilderUiState): RoutineCreateRequest {
        val scope = s.scope
        val f = s.form
        return RoutineCreateRequest(
            subjectIds = scope.subjectIds, chapterIds = scope.chapterIds, topicIds = scope.topicIds,
            questionsCount = f.questionsCount, durationMinutes = f.durationMinutes, markPerQuestion = f.markPerQuestion,
            negativeMarking = f.negativeMarking, negativeMarkValue = if (f.negativeMarking) f.negativeMarkValue else 0.0,
            groupId = s.owner.groupIdOrNull, durationDays = f.durationDays, weekdays = f.weekdays.sorted(),
            startDate = f.startDate, windowOpenMinute = f.windowOpenMinute, windowCloseMinute = f.windowCloseMinute,
        )
    }

    private fun runPreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            // After a restore the curriculum has to be back before the request can be built.
            chaptersJob?.join()
            _state.update { it.copy(isPreviewing = true, plan = null, previewProblem = null) }
            when (val r = previewRoutine(routineRequest(_state.value))) {
                is ApiResponse.Success -> _state.update { it.copy(isPreviewing = false, plan = r.data) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isPreviewing = false, previewProblem = PreviewProblem(errorMessages.messageFor(r), backToFor(r.errorCode)))
                }
            }
        }
    }

    fun retryPreview() = runPreview()

    private fun backToFor(code: String): BuilderStep = when (code) {
        GroupRoutineErrorCode.NO_TOPICS_TO_SCHEDULE, GroupRoutineErrorCode.CURRICULUM_MISMATCH, GroupRoutineErrorCode.SUBJECT_NOT_FOUND -> BuilderStep.CHAPTERS
        else -> BuilderStep.CONFIG
    }

    private fun saveRoutine() {
        val s = _state.value
        if (s.isSaving || s.plan == null) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val r = createRoutine(routineRequest(s))) {
                is ApiResponse.Success -> finish(GroupNotice(R.string.routine_created_group))
                is ApiResponse.Failure -> onSaveFailed(r)
            }
        }
    }

    private fun saveAdhoc() {
        val s = _state.value
        val groupId = s.owner.groupIdOrNull ?: return
        if (s.isSaving) return
        val scope = s.scope
        val f = s.form
        val body = AdhocExamRequest(
            subjectIds = scope.subjectIds, chapterIds = scope.chapterIds, topicIds = scope.topicIds,
            questionsCount = f.questionsCount, durationMinutes = f.durationMinutes, markPerQuestion = f.markPerQuestion,
            negativeMarking = f.negativeMarking, negativeMarkValue = if (f.negativeMarking) f.negativeMarkValue else 0.0,
            groupId = groupId, examDate = f.examDate, windowOpenMinute = f.windowOpenMinute, windowCloseMinute = f.windowCloseMinute,
            title = f.title.trim().ifEmpty { null },
        )
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val r = createAdhocExam(body)) {
                is ApiResponse.Success -> finish(GroupNotice(R.string.adhoc_created))
                is ApiResponse.Failure -> onSaveFailed(r)
            }
        }
    }

    private fun finish(notice: GroupNotice) {
        _state.value.owner.groupIdOrNull?.let { noticeBus.post(it, notice) }
        savedState.remove<String>(KEY_DRAFT)
        _state.update { it.copy(isSaving = false, isFinished = true) }
    }

    private fun onSaveFailed(r: ApiResponse.Failure) = _state.update {
        when (r.errorCode) {
            GroupRoutineErrorCode.NOT_GROUP_ADMIN, GroupRoutineErrorCode.GROUP_NOT_FOUND -> it.copy(isSaving = false, isFinished = true)
            else -> it.copy(
                isSaving = false,
                routineExists = it.routineExists || r.errorCode == GroupRoutineErrorCode.ROUTINE_EXISTS,
                errors = it.errors + (ERR_SAVE to errorMessages.messageFor(r)),
            )
        }
    }

    fun consumeFinished() = _state.update { it.copy(isFinished = false) }
    fun clearError(tag: String) = _state.update { it.copy(errors = it.errors - tag) }

    private companion object {
        const val KEY_DRAFT = "routine_builder_draft"
        const val ERR_SUBJECTS = "SUBJECTS"
        const val ERR_CURRICULUM = "CURRICULUM_"
        const val ERR_SAVE = "SAVE"
        val json = Json { ignoreUnknownKeys = true }
    }
}
