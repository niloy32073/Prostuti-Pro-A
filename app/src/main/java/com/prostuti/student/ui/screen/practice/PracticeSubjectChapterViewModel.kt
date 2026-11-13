package com.prostuti.student.ui.screen.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.domain.FetchChaptersUseCase
import com.prostuti.student.domain.FetchSubjectsUseCase
import com.prostuti.student.domain.FetchTopicsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val ERR_LOAD = "LOAD"

/**
 * Backs the Medium/Expanded combined screen (subject list + the focused subject's chapter/topic
 * tree, side by side — SubjectChapterMedium/Expanded in the design) that replaces the compact
 * flow's two separate steps on wider screens. Every selected subject's chapters/topics are loaded
 * (not just the focused one) so the final topic-id list is always complete — same reasoning as
 * [PracticeChapterSelectViewModel]: the backend ANDs topic_ids across every selected subject, so
 * a subject left at topic granularity for one and chapter-only for another would drop results.
 */
data class PracticeSubjectChapterUiState(
    val isLoadingSubjects: Boolean = true,
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectIds: Set<String> = emptySet(),
    val focusedSubjectId: String? = null,
    val loadingSubjectIds: Set<String> = emptySet(),
    val groupsBySubjectId: Map<String, PracticeSubjectGroup> = emptyMap(),
    val selectedTopicIds: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap()
) {
    val focusedGroup: PracticeSubjectGroup? get() = focusedSubjectId?.let { groupsBySubjectId[it] }

    val selectedChapterIds: Set<String>
        get() = groupsBySubjectId.values
            .flatMap { it.chapters }
            .filter { chapter -> chapter.topics.any { it.id in selectedTopicIds } }
            .map { it.chapter.id }
            .toSet()

    /** Needs a subject, and at least one ticked topic unless the loaded subjects have none at all. */
    val canProceed: Boolean
        get() = selectedSubjectIds.isNotEmpty() && loadingSubjectIds.isEmpty() &&
            (selectedTopicIds.isNotEmpty() || groupsBySubjectId.values.all { it.topicIds.isEmpty() })
}

class PracticeSubjectChapterViewModel(
    private val fetchSubjectsUseCase: FetchSubjectsUseCase,
    private val fetchChaptersUseCase: FetchChaptersUseCase,
    private val fetchTopicsUseCase: FetchTopicsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(PracticeSubjectChapterUiState())
    val state: StateFlow<PracticeSubjectChapterUiState> = _state.asStateFlow()

    init { loadSubjects() }

    private fun loadSubjects() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSubjects = true) }
            fetchSubjectsUseCase().first().let { r ->
                when (r) {
                    is ApiResponse.Success -> _state.update { it.copy(isLoadingSubjects = false, subjects = r.data) }
                    is ApiResponse.Failure -> _state.update {
                        it.copy(isLoadingSubjects = false, errors = it.errors + (ERR_LOAD to r.message))
                    }
                }
            }
        }
    }

    fun toggleSubject(subjectId: String) {
        val wasSelected = subjectId in _state.value.selectedSubjectIds
        _state.update { s ->
            val updated = if (wasSelected) s.selectedSubjectIds - subjectId else s.selectedSubjectIds + subjectId
            val focused = when {
                !wasSelected -> subjectId
                s.focusedSubjectId == subjectId -> updated.firstOrNull()
                else -> s.focusedSubjectId
            }
            s.copy(selectedSubjectIds = updated, focusedSubjectId = focused)
        }
        if (wasSelected) {
            dropSubjectData(subjectId)
        } else {
            loadSubjectChapters(subjectId)
        }
    }

    fun focusSubject(subjectId: String) {
        _state.update { it.copy(focusedSubjectId = subjectId) }
    }

    private fun dropSubjectData(subjectId: String) {
        _state.update { s ->
            val group = s.groupsBySubjectId[subjectId]
            val removedTopicIds = group?.chapters?.flatMap { it.topics.map { t -> t.id } }.orEmpty()
            s.copy(
                groupsBySubjectId = s.groupsBySubjectId - subjectId,
                selectedTopicIds = s.selectedTopicIds - removedTopicIds
            )
        }
    }

    private fun loadSubjectChapters(subjectId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loadingSubjectIds = it.loadingSubjectIds + subjectId) }

            val chapters = when (val r = fetchChaptersUseCase(subjectId).first()) {
                is ApiResponse.Success -> r.data
                is ApiResponse.Failure -> {
                    _state.update {
                        it.copy(
                            loadingSubjectIds = it.loadingSubjectIds - subjectId,
                            errors = it.errors + ("CHAPTERS_$subjectId" to r.message)
                        )
                    }
                    return@launch
                }
            }

            val chapterGroups = chapters.map { chapter ->
                async {
                    val topics = when (val r = fetchTopicsUseCase(chapter.id).first()) {
                        is ApiResponse.Success -> r.data
                        is ApiResponse.Failure -> {
                            _state.update { it.copy(errors = it.errors + ("TOPICS_${chapter.id}" to r.message)) }
                            emptyList()
                        }
                    }
                    PracticeChapterGroup(chapter = chapter, topics = topics)
                }
            }.awaitAll()

            val subject = _state.value.subjects.find { it.id == subjectId }
            val group = PracticeSubjectGroup(
                subjectId = subjectId, subjectNameBn = subject?.nameBn ?: subjectId, chapters = chapterGroups, subjectNameEn = subject?.nameEn.orEmpty()
            )
            val newTopicIds = chapterGroups.flatMap { c -> c.topics.map { it.id } }

            _state.update {
                it.copy(
                    loadingSubjectIds = it.loadingSubjectIds - subjectId,
                    groupsBySubjectId = it.groupsBySubjectId + (subjectId to group),
                    selectedTopicIds = it.selectedTopicIds + newTopicIds
                )
            }
        }
    }

    /** Subject checkbox in the right pane: clears or selects every topic of that subject in one tap. */
    fun toggleSubjectAll(subjectId: String) {
        _state.update { s ->
            val group = s.groupsBySubjectId[subjectId] ?: return@update s
            s.copy(selectedTopicIds = PracticeSelection.toggleAll(group.topicIds, s.selectedTopicIds))
        }
    }

    fun toggleChapterAll(subjectId: String, chapterId: String) {
        _state.update { s ->
            val chapter = s.groupsBySubjectId[subjectId]?.chapters?.find { it.chapter.id == chapterId } ?: return@update s
            s.copy(selectedTopicIds = PracticeSelection.toggleAll(chapter.topicIds, s.selectedTopicIds))
        }
    }

    fun toggleChapterOpen(subjectId: String, chapterId: String) {
        _state.update { s ->
            val group = s.groupsBySubjectId[subjectId] ?: return@update s
            val updatedGroup = group.copy(chapters = group.chapters.map { c ->
                if (c.chapter.id == chapterId) c.copy(isExpanded = !c.isExpanded) else c
            })
            s.copy(groupsBySubjectId = s.groupsBySubjectId + (subjectId to updatedGroup))
        }
    }

    fun toggleTopic(topicId: String) {
        _state.update { s -> s.copy(selectedTopicIds = PracticeSelection.toggleOne(topicId, s.selectedTopicIds)) }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
