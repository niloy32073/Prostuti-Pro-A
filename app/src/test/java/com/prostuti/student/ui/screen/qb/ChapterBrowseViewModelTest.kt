package com.prostuti.student.ui.screen.qb

import com.prostuti.student.data.repository.fake.FakeBookmarkRepository
import com.prostuti.student.domain.AddBookmarkUseCase
import com.prostuti.student.domain.ObserveBookmarkedIdsUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ChapterBrowseViewModel's questions are still dummy data (see its TODOs) — these tests cover its
 * selection/toggle logic, which is real even though the data source isn't. Bookmarks use a fake repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChapterBrowseViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val bookmarkRepo = FakeBookmarkRepository()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = ChapterBrowseViewModel("phy", ObserveBookmarkedIdsUseCase(bookmarkRepo), AddBookmarkUseCase(bookmarkRepo), RemoveBookmarkUseCase(bookmarkRepo))

    @Test
    fun `initial state selects the first chapter and loads its questions`() {
        val viewModel = buildViewModel()
        val state = viewModel.state.value

        assertEquals(8, state.chapters.size)
        assertEquals("ch1", state.selectedChapterId)
        assertTrue(state.questions.isNotEmpty())
        assertEquals(ChapterSourceFilter.ALL, state.sourceFilter)
    }

    @Test
    fun `selecting a chapter swaps its questions and clears revealed answers`() {
        val viewModel = buildViewModel()
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
        val viewModel = buildViewModel()
        viewModel.toggleAnswer(viewModel.state.value.questions.first().id)

        viewModel.selectSourceFilter(ChapterSourceFilter.BOARD)

        val state = viewModel.state.value
        assertEquals(ChapterSourceFilter.BOARD, state.sourceFilter)
        assertEquals("ch1", state.selectedChapterId)
        assertTrue(state.revealedAnswers.isEmpty())
    }

    @Test
    fun `toggleAnswer is a set toggle`() {
        val viewModel = buildViewModel()
        val qid = viewModel.state.value.questions.first().id

        viewModel.toggleAnswer(qid)
        assertTrue(qid in viewModel.state.value.revealedAnswers)

        viewModel.toggleAnswer(qid)
        assertTrue(qid !in viewModel.state.value.revealedAnswers)
    }

    @Test
    fun `toggleFavorite is a set toggle independent of revealed answers`() {
        val viewModel = buildViewModel()
        val qid = viewModel.state.value.questions.first().id

        viewModel.toggleFavorite(qid)
        assertTrue(qid in viewModel.state.value.favoriteIds)
        viewModel.toggleFavorite(qid)
        assertTrue(qid !in viewModel.state.value.favoriteIds)
    }
}
