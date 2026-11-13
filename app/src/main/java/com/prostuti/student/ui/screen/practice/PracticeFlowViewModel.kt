package com.prostuti.student.ui.screen.practice

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the practice exam covers. chapterIds always come from topicIds (see PracticeChapterSelectUiState). */
data class PracticeScope(
    val subjectIds: List<String>,
    val chapterIds: List<String>,
    val topicIds: List<String>,
)

/**
 * The practice builder's picks, shared by its screens so the (possibly hundreds of) topic ids
 * never go into a route. Observable, so exam properties already open beside the picks (wide
 * windows) follow a new Next. In memory only: [scope] is null again after process death.
 */
class PracticeFlowViewModel : ViewModel() {
    private val _scope = MutableStateFlow<PracticeScope?>(null)
    val scope: StateFlow<PracticeScope?> = _scope.asStateFlow()

    fun setScope(scope: PracticeScope) {
        _scope.value = scope
    }
}
