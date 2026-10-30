package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CreateGroupRoutineRequest
import com.prostuti.student.domain.CreateGroupRoutineUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 0=সোম..6=রবি, matching [com.prostuti.student.data.model.response.GroupRoutineSummary]'s bitmask. */
val weekDayLabelsBn = listOf("সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি", "রবি")

data class GroupRoutineWindowUiState(
    val routineDurationDays: Int = 30,
    val examWeekDays: Set<Int> = setOf(2, 5), // বুধ ও শনি, matching the design's default
    val questionCount: Int = 25,
    val markPerQuestion: Float = 1f,
    val negativeMarkingEnabled: Boolean = true,
    val negativeMark: Float = 0.25f,
    val examDurationMinutes: Int = 30,
    val windowStartMinuteOfDay: Int = 19 * 60,
    val windowEndMinuteOfDay: Int = 23 * 60,
    val locked: Boolean = false,
    val isSaving: Boolean = false,
    val errorBn: String? = null
) {
    val totalMarks: Float get() = questionCount * markPerQuestion
    val canSave: Boolean get() = examWeekDays.isNotEmpty() && windowEndMinuteOfDay > windowStartMinuteOfDay
}

/** Step 3/3 — once saved the routine is locked forever (no edit use case exists at all). */
class GroupRoutineWindowViewModel(
    private val groupId: String,
    private val subjectIds: List<String>,
    private val chapterIds: List<String>,
    private val topicIds: List<String>,
    private val createGroupRoutineUseCase: CreateGroupRoutineUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(GroupRoutineWindowUiState())
    val state: StateFlow<GroupRoutineWindowUiState> = _state.asStateFlow()

    fun changeDurationDays(delta: Int) = updateUnlocked { it.copy(routineDurationDays = (it.routineDurationDays + delta).coerceIn(7, 90)) }

    fun toggleWeekDay(dayIndex: Int) = updateUnlocked {
        it.copy(examWeekDays = if (dayIndex in it.examWeekDays) it.examWeekDays - dayIndex else it.examWeekDays + dayIndex)
    }

    fun changeQuestionCount(delta: Int) = updateUnlocked { it.copy(questionCount = (it.questionCount + delta).coerceAtLeast(5)) }

    fun changeMarkPerQuestion(delta: Float) = updateUnlocked {
        it.copy(markPerQuestion = ((it.markPerQuestion + delta) * 100).roundToInt().coerceAtLeast(25) / 100f)
    }

    fun toggleNegativeMarking() = updateUnlocked { it.copy(negativeMarkingEnabled = !it.negativeMarkingEnabled) }

    fun changeNegativeMark(delta: Float) = updateUnlocked {
        if (!it.negativeMarkingEnabled) it else it.copy(negativeMark = ((it.negativeMark + delta) * 100).roundToInt().coerceAtLeast(0) / 100f)
    }

    fun changeExamDuration(delta: Int) = updateUnlocked { it.copy(examDurationMinutes = (it.examDurationMinutes + delta).coerceIn(5, 180)) }

    fun changeWindowStart(deltaMinutes: Int) = updateUnlocked {
        it.copy(windowStartMinuteOfDay = (it.windowStartMinuteOfDay + deltaMinutes).coerceIn(0, 23 * 60 + 30))
    }

    fun changeWindowEnd(deltaMinutes: Int) = updateUnlocked {
        it.copy(windowEndMinuteOfDay = (it.windowEndMinuteOfDay + deltaMinutes).coerceIn(30, 24 * 60))
    }

    private inline fun updateUnlocked(block: (GroupRoutineWindowUiState) -> GroupRoutineWindowUiState) {
        _state.update { if (it.locked) it else block(it) }
    }

    fun save() {
        val s = _state.value
        if (!s.canSave || s.locked) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorBn = null) }
            val bitmask = s.examWeekDays.fold(0) { acc, day -> acc or (1 shl day) }
            val request = CreateGroupRoutineRequest(
                subjectIds = subjectIds, chapterIds = chapterIds, topicIds = topicIds,
                routineDurationDays = s.routineDurationDays, examWeekDaysBitmask = bitmask,
                questionCount = s.questionCount, markPerQuestion = s.markPerQuestion,
                negativeMarkingEnabled = s.negativeMarkingEnabled, negativeMark = s.negativeMark,
                examDurationMinutes = s.examDurationMinutes,
                windowStartMinuteOfDay = s.windowStartMinuteOfDay, windowEndMinuteOfDay = s.windowEndMinuteOfDay
            )
            when (val r = createGroupRoutineUseCase(groupId, request)) {
                is ApiResponse.Success -> _state.update { it.copy(isSaving = false, locked = true) }
                is ApiResponse.Failure -> _state.update { it.copy(isSaving = false, errorBn = r.message) }
            }
        }
    }

    fun clearError() { _state.update { it.copy(errorBn = null) } }
}
