package com.prostuti.student.ui.screen.bookmark

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.McqDetail
import com.prostuti.student.data.repository.MCQRepository
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.data.repository.fake.FakeBookmarkRepository
import com.prostuti.student.data.repository.fake.FakeBookmarkRepository.Companion.bookmark
import com.prostuti.student.domain.FetchMcqUseCase
import com.prostuti.student.domain.ObserveBookmarksUseCase
import com.prostuti.student.domain.RemoveBookmarkUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var bookmarkRepo: FakeBookmarkRepository

    /** mcqId -> what GET questions/mcq/{id} returns; anything missing fails. */
    private val mcqResults = mutableMapOf<String, ApiResponse<McqDetail>>()
    private val fetchedMcqIds = mutableListOf<String>()

    private val mcqRepo: MCQRepository = object : MCQRepository by FakeExamAttemptRepositoryForEngineTest() {
        override suspend fun fetchMcq(mcqId: String): ApiResponse<McqDetail> {
            fetchedMcqIds += mcqId
            return mcqResults[mcqId] ?: ApiResponse.Failure(message = "Not found", errorCode = "NOT_FOUND")
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bookmarkRepo = FakeBookmarkRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = BookmarkHistoryViewModel(
        observeBookmarksUseCase = ObserveBookmarksUseCase(bookmarkRepo),
        fetchMcqUseCase = FetchMcqUseCase(mcqRepo),
        removeBookmarkUseCase = RemoveBookmarkUseCase(bookmarkRepo)
    )

    private fun mcq(id: String) = ApiResponse.Success(McqDetail(id = id, questionText = "Question $id"))

    @Test fun `loads the bookmarks and the question of each`() = runTest {
        bookmarkRepo.bookmarks.value = listOf(bookmark("m1"), bookmark("m2"))
        mcqResults["m1"] = mcq("m1")
        mcqResults["m2"] = mcq("m2")

        val state = buildViewModel().state.value

        assertTrue(state.hasLoaded)
        assertFalse(state.isLoading)
        assertEquals(listOf("m1", "m2"), state.bookmarks.map { it.resourceId })
        assertEquals("Question m2", state.mcqs["m2"]?.questionText)
        assertEquals(false, bookmarkRepo.lastRefresh)
    }

    @Test fun `a question that fails to load is marked and can be retried`() = runTest {
        bookmarkRepo.bookmarks.value = listOf(bookmark("m1"))
        val viewModel = buildViewModel()
        assertEquals(setOf("m1"), viewModel.state.value.failedMcqIds)

        mcqResults["m1"] = mcq("m1")
        viewModel.retryMcq("m1")

        assertTrue(viewModel.state.value.failedMcqIds.isEmpty())
        assertEquals("Question m1", viewModel.state.value.mcqs["m1"]?.questionText)
    }

    @Test fun `each question downloads only once`() = runTest {
        bookmarkRepo.bookmarks.value = listOf(bookmark("m1"))
        mcqResults["m1"] = mcq("m1")
        buildViewModel()

        bookmarkRepo.bookmarks.value = listOf(bookmark("m2")) + bookmarkRepo.bookmarks.value
        mcqResults["m2"] = mcq("m2")

        assertEquals(listOf("m1", "m2"), fetchedMcqIds)
    }

    @Test fun `removing a bookmark drops its card`() = runTest {
        bookmarkRepo.bookmarks.value = listOf(bookmark("m1"), bookmark("m2"))
        val viewModel = buildViewModel()

        viewModel.removeBookmark("m1")

        assertEquals(listOf(BookmarkResourceType.MCQ to "m1"), bookmarkRepo.removed)
        assertEquals(listOf("m2"), viewModel.state.value.bookmarks.map { it.resourceId })
    }

    @Test fun `a failed remove brings the card back and shows the error`() = runTest {
        bookmarkRepo.bookmarks.value = listOf(bookmark("m1"))
        bookmarkRepo.removeResult = ApiResponse.Failure(message = "No internet connection", errorCode = "NETWORK_ERROR")
        val viewModel = buildViewModel()

        viewModel.removeBookmark("m1")

        assertEquals(listOf("m1"), viewModel.state.value.bookmarks.map { it.resourceId })
        assertEquals("No internet connection", viewModel.state.value.errors["REMOVE"])
    }

    @Test fun `a failed list load shows the error`() = runTest {
        bookmarkRepo.loadFailure = ApiResponse.Failure(message = "Timeout: Server is too slow", errorCode = "TIMEOUT_ERROR")

        val state = buildViewModel().state.value

        assertEquals("Timeout: Server is too slow", state.errors["LOAD"])
        assertFalse(state.isLoading)
    }

    @Test fun `refresh asks for a fresh list`() = runTest {
        val viewModel = buildViewModel()

        viewModel.refresh()

        assertEquals(true, bookmarkRepo.lastRefresh)
    }

    @Test fun `toggleAnswer opens and closes one card`() = runTest {
        val viewModel = buildViewModel()

        viewModel.toggleAnswer("m1")
        assertEquals(setOf("m1"), viewModel.state.value.revealedMcqIds)
        viewModel.toggleAnswer("m1")
        assertTrue(viewModel.state.value.revealedMcqIds.isEmpty())
    }

    @Test fun `clearError removes only that error`() = runTest {
        bookmarkRepo.loadFailure = ApiResponse.Failure(message = "x", errorCode = "E")
        val viewModel = buildViewModel()

        viewModel.clearError("LOAD")

        assertTrue(viewModel.state.value.errors.isEmpty())
    }
}
