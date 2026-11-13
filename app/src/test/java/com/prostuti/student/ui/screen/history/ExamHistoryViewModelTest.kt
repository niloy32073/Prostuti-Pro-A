package com.prostuti.student.ui.screen.history

import app.cash.turbine.test
import com.prostuti.student.data.model.enums.HistoryStatus
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ExamHistoryItem
import com.prostuti.student.data.model.response.MetaData
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest
import com.prostuti.student.data.repository.fake.FakeQbCurriculumRepository
import com.prostuti.student.domain.FetchSubjectsUseCase
import com.prostuti.student.domain.GetExamHistoryUseCase
import com.prostuti.student.domain.HistoryTimeRange
import com.prostuti.student.util.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExamHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FakeExamAttemptRepositoryForEngineTest
    private val subjects = listOf(Subject("s1", "পদার্থবিজ্ঞান", "Physics", createdAt = "2026-01-01", createdBy = "x"))
    private val time = FakeTimeProvider(Instant.parse("2026-11-08T06:00:00Z").toEpochMilli())

    private fun item(id: String, status: String = HistoryStatus.COMPLETED) = ExamHistoryItem(
        examId = "e_$id", attemptId = id, examTitleBn = "এক্সাম $id", attemptedAt = "2026-11-05T04:00:00Z", status = status
    )

    private fun page(vararg ids: String, totalPages: Int) =
        ApiResponse.Success(ids.map { item(it) }, metaData = MetaData(currentPage = 1, totalPages = totalPages, totalItems = 42))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = FakeExamAttemptRepositoryForEngineTest(historyResult = page("a1", "a2", totalPages = 2))
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun buildViewModel() = ExamHistoryViewModel(
        GetExamHistoryUseCase(repo),
        FetchSubjectsUseCase(FakeQbCurriculumRepository(ApiResponse.Success(subjects))),
        time
    )

    @Test
    fun `first page loads with no filters, subjects and total count`() = runTest {
        val vm = buildViewModel()

        val s = vm.state.value
        assertFalse(s.isLoading)
        assertEquals(listOf("a1", "a2"), s.items.map { it.attemptId })
        assertTrue(s.hasMore)
        assertEquals(42, s.totalItems)
        assertEquals(subjects, s.subjects)
        val (filter, pageNo) = repo.historyCalls.single()
        assertEquals(1, pageNo)
        assertNull(filter.status)
        assertNull(filter.subjectId)
        assertNull(filter.dateFrom)
    }

    @Test
    fun `loadMore appends the next page and stops at the last page`() = runTest {
        val vm = buildViewModel()
        repo.historyResult = ApiResponse.Success(listOf(item("a3")), metaData = MetaData(currentPage = 2, totalPages = 2))

        vm.loadMore()

        assertEquals(listOf("a1", "a2", "a3"), vm.state.value.items.map { it.attemptId })
        assertFalse(vm.state.value.hasMore)
        assertEquals(2, repo.historyCalls.last().second)

        vm.loadMore()
        assertEquals(2, repo.historyCalls.size)   // no request past the last page
    }

    @Test
    fun `changing a filter reloads page 1 with that filter`() = runTest {
        val vm = buildViewModel()

        vm.setStatus(HistoryStatus.MISSED)
        vm.setSubject("s1")
        vm.setTimeRange(HistoryTimeRange.LAST_7_DAYS)

        val (filter, pageNo) = repo.historyCalls.last()
        assertEquals(1, pageNo)
        assertEquals(HistoryStatus.MISSED, filter.status)
        assertEquals("s1", filter.subjectId)
        assertNotNull(filter.dateFrom)
        assertNotNull(filter.dateTo)
        assertTrue(vm.state.value.hasFilter)
        assertEquals(4, repo.historyCalls.size)
    }

    @Test
    fun `custom range is sent as Dhaka day bounds and reset clears every filter`() = runTest {
        val vm = buildViewModel()

        vm.setCustomRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3))
        assertEquals("2026-10-31T18:00:00", repo.historyCalls.last().first.dateFrom)
        assertEquals(HistoryTimeRange.CUSTOM, vm.state.value.timeRange)

        vm.resetFilters()
        val s = vm.state.value
        assertFalse(s.hasFilter)
        assertNull(s.customStart)
        assertNull(repo.historyCalls.last().first.dateFrom)
    }

    @Test
    fun `setting the same filter again does not reload`() = runTest {
        val vm = buildViewModel()
        vm.setStatus(null)
        vm.setTimeRange(HistoryTimeRange.ALL)
        vm.resetFilters()
        assertEquals(1, repo.historyCalls.size)
    }

    @Test
    fun `failure shows LOAD error and keeps loading off`() = runTest {
        repo.historyResult = ApiResponse.Failure("date_from must not be after date_to.", "INVALID_DATE_RANGE")
        val vm = buildViewModel()

        vm.state.test {
            val s = awaitItem()
            assertFalse(s.isLoading)
            assertEquals("date_from must not be after date_to.", s.errors["LOAD"])
            vm.clearError("LOAD")
            assertNull(awaitItem().errors["LOAD"])
        }
    }

    @Test
    fun `refresh keeps current items until the new page arrives`() = runTest {
        val vm = buildViewModel()
        repo.historyResult = page("a9", totalPages = 1)

        vm.refresh()

        assertEquals(listOf("a9"), vm.state.value.items.map { it.attemptId })
        assertFalse(vm.state.value.isRefreshing)
    }
}
