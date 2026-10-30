package com.prostuti.student.ui.screen.qb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ChapterBrowseViewModel is currently dummy-data-driven (no repository yet, see its TODOs) —
 * these tests cover its selection/toggle logic, which is real even though the data source isn't. */
class ChapterBrowseViewModelTest {

    @Test
    fun `initial state selects the first chapter and loads its questions`() {
        val viewModel = ChapterBrowseViewModel("phy")
        val state = viewModel.state.value

        assertEquals(8, state.chapters.size)
        assertEquals("ch1", state.selectedChapterId)
        assertTrue(state.questions.isNotEmpty())
        assertEquals(ChapterSourceFilter.ALL, state.sourceFilter)
    }

    @Test
    fun `selecting a chapter swaps its questions and clears revealed answers`() {
        val viewModel = ChapterBrowseViewModel("phy")
        viewModel.toggleAnswer(viewModel.state.value.questions.first().id)
        assertTrue(viewModel.state.value.revealedAnswers.isNotEmpty())

        viewModel.selectChapter("ch2")

        val state = viewModel.state.value
        assertEquals("ch2", state.selectedChapterId)
        assertTrue(state.revealedAnswers.isEmpty())
        assertTrue(state.questions.all { it.id.startsWith("ch2") })
    }

    @Test
    fun `changing the source filter clears revealed answers but keeps the chapter`() {
        val viewModel = ChapterBrowseViewModel("phy")
        viewModel.toggleAnswer(viewModel.state.value.questions.first().id)

        viewModel.selectSourceFilter(ChapterSourceFilter.BOARD)

        val state = viewModel.state.value
        assertEquals(ChapterSourceFilter.BOARD, state.sourceFilter)
        assertEquals("ch1", state.selectedChapterId)
        assertTrue(state.revealedAnswers.isEmpty())
    }

    @Test
    fun `toggleAnswer is a set toggle`() {
        val viewModel = ChapterBrowseViewModel("phy")
        val qid = viewModel.state.value.questions.first().id

        viewModel.toggleAnswer(qid)
        assertTrue(qid in viewModel.state.value.revealedAnswers)

        viewModel.toggleAnswer(qid)
        assertTrue(qid !in viewModel.state.value.revealedAnswers)
    }

    @Test
    fun `toggleFavorite is a set toggle independent of revealed answers`() {
        val viewModel = ChapterBrowseViewModel("phy")
        val qid = viewModel.state.value.questions.first().id

        viewModel.toggleFavorite(qid)
        assertTrue(qid in viewModel.state.value.favoriteIds)
        viewModel.toggleFavorite(qid)
        assertTrue(qid !in viewModel.state.value.favoriteIds)
    }
}
