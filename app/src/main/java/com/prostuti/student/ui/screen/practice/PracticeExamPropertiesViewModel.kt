package com.prostuti.student.ui.screen.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.enums.Level
import com.prostuti.student.data.model.request.ExamGenerateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.GenerateExamUseCase
import com.prostuti.student.ui.screen.examengine.ExamAttemptStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val QUESTION_COUNT_MIN = 5
const val QUESTION_COUNT_MAX = 100
val QUESTION_COUNT_PRESETS = listOf("25", "35", "40", "50", "100")
val DURATION_PRESETS = listOf("15", "20", "25", "30", "45", "60")
val MARK_PER_QUESTION_PRESETS = listOf("0.5", "1", "2", "4")
val NEGATIVE_MARK_PRESETS = listOf("0.1", "0.25", "0.5", "1")

/** Source chips (বোর্ড/কলেজ/অ্যাডমিশন/প্রস্তুতি অরিজিনাল) — kept client-side only: the backend's
 *  ExamGenerateRequest has no matching field yet, so this is collected and shown but not sent. */
enum class PracticeQuestionSource { BOARD, COLLEGE, ADMISSION, ORIGINAL }

data class PracticeExamPropertiesUiState(
    val questionCountText: String = "20",
    val level: Level = Level.ACADEMIC,
    val version: String = "BN",
    val selectedSources: Set<PracticeQuestionSource> = setOf(PracticeQuestionSource.BOARD, PracticeQuestionSource.COLLEGE, PracticeQuestionSource.ORIGINAL),
    val durationText: String = "25",
    val markPerQuestionText: String = "1",
    val negativeMarkingEnabled: Boolean = true,
    val negativeMarkText: String = "0.25",
    val isGenerating: Boolean = false,
    val generatedAttemptId: String? = null,
    val errors: Map<String, String> = emptyMap()
) {
    val questionCount: Int get() = questionCountText.toIntOrNull()?.coerceIn(QUESTION_COUNT_MIN, QUESTION_COUNT_MAX) ?: QUESTION_COUNT_MIN
    val durationMinutes: Int? get() = durationText.toIntOrNull()
    val markPerQuestion: Double? get() = markPerQuestionText.toDoubleOrNull()
    val negativeMark: Double? get() = if (negativeMarkingEnabled) negativeMarkText.toDoubleOrNull() else 0.0
}

class PracticeExamPropertiesViewModel(
    private val subjectIds: List<String>,
    private val chapterIds: List<String>,
    private val topicIds: List<String>,
    private val generateExamUseCase: GenerateExamUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(PracticeExamPropertiesUiState())
    val state: StateFlow<PracticeExamPropertiesUiState> = _state.asStateFlow()

    fun setQuestionCountText(text: String) = setDigitsOnly(text) { _state.update { it.copy(questionCountText = text) } }
    fun pickQuestionCountPreset(value: String) { _state.update { it.copy(questionCountText = value) } }

    fun setDurationText(text: String) = setDigitsOnly(text) { _state.update { it.copy(durationText = text) } }
    fun pickDurationPreset(value: String) { _state.update { it.copy(durationText = value) } }

    fun setMarkPerQuestionText(text: String) = setDecimalOnly(text) { _state.update { it.copy(markPerQuestionText = text) } }
    fun pickMarkPerQuestionPreset(value: String) { _state.update { it.copy(markPerQuestionText = value) } }

    fun setNegativeMarkText(text: String) = setDecimalOnly(text) { _state.update { it.copy(negativeMarkText = text) } }
    fun pickNegativeMarkPreset(value: String) { _state.update { it.copy(negativeMarkText = value) } }
    fun setNegativeMarkingEnabled(enabled: Boolean) { _state.update { it.copy(negativeMarkingEnabled = enabled) } }

    fun setLevel(level: Level) { _state.update { it.copy(level = level) } }
    fun setVersion(version: String) { _state.update { it.copy(version = version) } }

    fun toggleSource(source: PracticeQuestionSource) {
        _state.update { s ->
            val updated = if (source in s.selectedSources) s.selectedSources - source else s.selectedSources + source
            s.copy(selectedSources = updated)
        }
    }

    private inline fun setDigitsOnly(text: String, apply: () -> Unit) {
        if (text.isEmpty() || text.all { it.isDigit() }) apply()
    }

    private inline fun setDecimalOnly(text: String, apply: () -> Unit) {
        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) apply()
    }

    fun generateExam() {
        if (_state.value.isGenerating) return
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true) }
            val s = _state.value
            val request = ExamGenerateRequest(
                subjectIds = subjectIds.ifEmpty { null },
                chapterIds = chapterIds.ifEmpty { null },
                topicIds = topicIds.ifEmpty { null },
                questionCount = s.questionCount,
                durationMinutes = s.durationMinutes,
                level = s.level,
                version = s.version,
                markPerQuestion = s.markPerQuestion,
                negativeMark = s.negativeMark
            )
            when (val r = generateExamUseCase(request)) {
                is ApiResponse.Success -> {
                    ExamAttemptStore.put(r.data)
                    _state.update { it.copy(isGenerating = false, generatedAttemptId = r.data.attemptId) }
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isGenerating = false, errors = it.errors + ("GENERATE" to r.message))
                }
            }
        }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
