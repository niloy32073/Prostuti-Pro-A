package com.prostuti.student.ui.screen.practice

import androidx.compose.ui.state.ToggleableState
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSelectionTest {

    private fun topic(id: String, chapterId: String) =
        Topic(id = id, chapterId = chapterId, nameBn = id, nameEn = id, order = 0, createdAt = "", createdBy = "")

    private fun chapter(id: String, vararg topicIds: String) = PracticeChapterGroup(
        chapter = Chapter(id = id, subjectId = "s", chapterNameBn = id, chapterNameEn = id, order = 0, createdAt = "", createdBy = ""),
        topics = topicIds.map { topic(it, id) }
    )

    // Subject "s": chapter c1 = {t1, t2}, chapter c2 = {t3}
    private val subject = PracticeSubjectGroup("s", "বিষয়", listOf(chapter("c1", "t1", "t2"), chapter("c2", "t3")))
    private val all = setOf("t1", "t2", "t3")

    @Test fun stateIsOffWhenNoneSelected() {
        assertEquals(ToggleableState.Off, subject.toggleState(emptySet()))
        assertEquals(ToggleableState.Off, subject.chapters[0].toggleState(setOf("t3")))
    }

    @Test fun stateIsIndeterminateWhenSomeSelected() {
        assertEquals(ToggleableState.Indeterminate, subject.toggleState(setOf("t1")))
        assertEquals(ToggleableState.Indeterminate, subject.chapters[0].toggleState(setOf("t2")))
    }

    @Test fun stateIsOnWhenAllSelected() {
        assertEquals(ToggleableState.On, subject.toggleState(all))
        assertEquals(ToggleableState.On, subject.chapters[1].toggleState(setOf("t3")))
    }

    @Test fun emptyParentIsOff() {
        assertEquals(ToggleableState.Off, chapter("empty").toggleState(all))
    }

    @Test fun tappingOnSubjectClearsAllItsTopicsAndKeepsOthers() {
        val result = PracticeSelection.toggleAll(subject.topicIds, all + "other")
        assertEquals(setOf("other"), result)
    }

    @Test fun tappingOffOrIndeterminateSubjectSelectsAll() {
        assertEquals(all, PracticeSelection.toggleAll(subject.topicIds, emptySet()))
        assertEquals(all, PracticeSelection.toggleAll(subject.topicIds, setOf("t2")))
    }

    @Test fun subjectRoundTripClearThenSelect() {
        val cleared = PracticeSelection.toggleAll(subject.topicIds, all)
        assertEquals(ToggleableState.Off, subject.toggleState(cleared))
        val reselected = PracticeSelection.toggleAll(subject.topicIds, cleared)
        assertEquals(ToggleableState.On, subject.toggleState(reselected))
    }

    @Test fun childChangesUpdateParentState() {
        var selected = emptySet<String>()
        selected = PracticeSelection.toggleOne("t1", selected)
        assertEquals(ToggleableState.Indeterminate, subject.toggleState(selected))
        assertEquals(ToggleableState.Indeterminate, subject.chapters[0].toggleState(selected))
        selected = PracticeSelection.toggleOne("t2", selected)
        assertEquals(ToggleableState.On, subject.chapters[0].toggleState(selected))
        assertEquals(ToggleableState.Indeterminate, subject.toggleState(selected))
        selected = PracticeSelection.toggleAll(subject.chapters[1].topicIds, selected)
        assertEquals(ToggleableState.On, subject.toggleState(selected))
        selected = PracticeSelection.toggleOne("t1", selected)
        assertEquals(ToggleableState.Indeterminate, subject.toggleState(selected))
    }

    @Test fun nextIsBlockedWhenNothingTicked() {
        assertFalse(PracticeChapterSelectUiState(isLoading = false, groups = listOf(subject)).canProceed)
        assertTrue(PracticeChapterSelectUiState(isLoading = false, groups = listOf(subject), selectedTopicIds = setOf("t1")).canProceed)

        val twoPane = PracticeSubjectChapterUiState(
            isLoadingSubjects = false,
            selectedSubjectIds = setOf("s"),
            groupsBySubjectId = mapOf("s" to subject)
        )
        assertFalse(twoPane.canProceed)
        assertTrue(twoPane.copy(selectedTopicIds = setOf("t3")).canProceed)
    }
}
