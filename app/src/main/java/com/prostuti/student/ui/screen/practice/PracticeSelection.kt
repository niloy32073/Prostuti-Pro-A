package com.prostuti.student.ui.screen.practice

import androidx.compose.ui.state.ToggleableState

/**
 * Tri-state checkbox logic shared by the compact chapter/topic screen and the two-pane screen.
 * Selection is stored only as a set of topic ids; a subject's or chapter's state is derived from
 * how many of its topics are in that set.
 */
internal object PracticeSelection {

    /** Off when none are selected, On when all are, Indeterminate otherwise. */
    fun stateOf(topicIds: Collection<String>, selected: Set<String>): ToggleableState {
        val count = topicIds.count { it in selected }
        return when {
            count == 0 -> ToggleableState.Off
            count == topicIds.size -> ToggleableState.On
            else -> ToggleableState.Indeterminate
        }
    }

    /** An On parent clears all its topics; an Off or Indeterminate parent selects all of them. */
    fun toggleAll(topicIds: Collection<String>, selected: Set<String>): Set<String> =
        if (stateOf(topicIds, selected) == ToggleableState.On) selected - topicIds.toSet()
        else selected + topicIds

    fun toggleOne(topicId: String, selected: Set<String>): Set<String> =
        if (topicId in selected) selected - topicId else selected + topicId
}

internal val PracticeChapterGroup.topicIds: List<String> get() = topics.map { it.id }

internal val PracticeSubjectGroup.topicIds: List<String> get() = chapters.flatMap { it.topicIds }

internal fun PracticeChapterGroup.toggleState(selected: Set<String>) =
    PracticeSelection.stateOf(topicIds, selected)

internal fun PracticeSubjectGroup.toggleState(selected: Set<String>) =
    PracticeSelection.stateOf(topicIds, selected)
