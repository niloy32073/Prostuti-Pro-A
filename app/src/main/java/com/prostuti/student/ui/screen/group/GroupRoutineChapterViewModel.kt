package com.prostuti.student.ui.screen.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.FetchChaptersUseCase
import com.prostuti.student.domain.FetchSubjectsUseCase
import com.prostuti.student.domain.FetchTopicsUseCase
import com.prostuti.student.ui.screen.practice.PracticeChapterGroup
import com.prostuti.student.ui.screen.practice.PracticeSelection
import com.prostuti.student.ui.screen.practice.PracticeSubjectGroup
import com.prostuti.student.ui.screen.practice.topicIds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupRoutineChapterUiState(
    val isLoading: Boolean = true,
    val groups: List<PracticeSubjectGroup> = emptyList(),
    val selectedTopicIds: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap()
) {
    val totalTopicCount get() = groups.sumOf { g -> g.chapters.sumOf { it.topics.size } }

    val selectedChapterIds: Set<String>
        get() = groups.flatMap { it.chapters }
            .filter { chapter -> chapter.topics.any { it.id in selectedTopicIds } }
            .map { it.chapter.id }.toSet()

    val canProceed: Boolean get() = selectedTopicIds.isNotEmpty()
}

/**
 * Step 2/3 of the group-routine builder — reuses the exact tri-state selection model and UI
 * ([PracticeSubjectGroup]/[PracticeChapterGroup]/[PracticeSelection],
 * [com.prostuti.student.ui.screen.practice.ChapterCard]/`SubjectSelectHeader`) that the practice
 * builder already has, since the "pick subjects → pick chapters/topics" mechanics are identical.
 * Every topic starts selected ("পুরো বই"), unlike practice where nothing is pre-picked, since a
 * group routine more often covers whole books.
 */
class GroupRoutineChapterViewModel(
    val groupId: String,
    private val subjectIds: List<String>,
    private val fetchSubjectsUseCase: FetchSubjectsUseCase,
    private val fetchChaptersUseCase: FetchChaptersUseCase,
    private val fetchTopicsUseCase: FetchTopicsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(GroupRoutineChapterUiState())
    val state: StateFlow<GroupRoutineChapterUiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val subjectsById = when (val r = fetchSubjectsUseCase().first()) {
                is ApiResponse.Success -> r.data.associateBy { it.id }
                is ApiResponse.Failure -> {
                    _state.update { it.copy(isLoading = false, errors = it.errors + ("LOAD" to r.message)) }
                    return@launch
                }
            }

            val groups = subjectIds.map { subjectId ->
                async {
                    val chapters = when (val r = fetchChaptersUseCase(subjectId).first()) {
                        is ApiResponse.Success -> r.data
                        is ApiResponse.Failure -> {
                            _state.update { it.copy(errors = it.errors + ("CHAPTERS_$subjectId" to r.message)) }
                            emptyList()
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
                    PracticeSubjectGroup(subjectId = subjectId, subjectNameBn = subjectsById[subjectId]?.nameBn ?: subjectId, chapters = chapterGroups)
                }
            }.awaitAll()

            val allTopicIds = groups.flatMap { g -> g.chapters.flatMap { c -> c.topics.map { it.id } } }.toSet()
            _state.update { it.copy(isLoading = false, groups = groups, selectedTopicIds = allTopicIds) }
        }
    }

    fun toggleChapterOpen(chapterId: String) {
        _state.update { s ->
            s.copy(groups = s.groups.map { g ->
                g.copy(chapters = g.chapters.map { c -> if (c.chapter.id == chapterId) c.copy(isExpanded = !c.isExpanded) else c })
            })
        }
    }

    fun toggleSubjectAll(subjectId: String) {
        _state.update { s ->
            val group = s.groups.find { it.subjectId == subjectId } ?: return@update s
            s.copy(selectedTopicIds = PracticeSelection.toggleAll(group.topicIds, s.selectedTopicIds))
        }
    }

    fun toggleChapterAll(chapterId: String) {
        _state.update { s ->
            val chapter = s.groups.flatMap { it.chapters }.find { it.chapter.id == chapterId } ?: return@update s
            s.copy(selectedTopicIds = PracticeSelection.toggleAll(chapter.topicIds, s.selectedTopicIds))
        }
    }

    fun toggleTopic(topicId: String) {
        _state.update { s -> s.copy(selectedTopicIds = PracticeSelection.toggleOne(topicId, s.selectedTopicIds)) }
    }

    fun clearError(tag: String) { _state.update { it.copy(errors = it.errors - tag) } }
}
