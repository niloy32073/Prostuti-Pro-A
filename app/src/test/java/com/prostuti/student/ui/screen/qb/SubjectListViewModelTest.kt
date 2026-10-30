package com.prostuti.student.ui.screen.qb

import app.cash.turbine.test
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.repository.fake.FakeQbCurriculumRepository
import com.prostuti.student.domain.FetchSubjectsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class SubjectListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun fakeSubject(id: String) = Subject(
        id = id, nameBn = "বিষয়-$id", nameEn = "Subject-$id", createdAt = "2025-01-01", createdBy = "admin"
    )

    @Test
    fun `initial load success populates subjects and clears loading`() = runTest {
        val repo = FakeQbCurriculumRepository(
            subjectsResult = ApiResponse.Success(listOf(fakeSubject("s1"), fakeSubject("s2")))
        )
        val viewModel = SubjectListViewModel(FetchSubjectsUseCase(repo))

        viewModel.state.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(2, loaded.subjects.size)
            assertEquals("s1", loaded.subjects[0].id)
        }
        assertEquals(false, repo.lastRefreshValue)
    }

    @Test
    fun `initial load failure populates errors map under LOAD tag`() = runTest {
        val repo = FakeQbCurriculumRepository(subjectsResult = ApiResponse.Failure("নেটওয়ার্ক সমস্যা", "NETWORK_ERROR"))
        val viewModel = SubjectListViewModel(FetchSubjectsUseCase(repo))

        viewModel.state.test {
            skipItems(1) // loading
            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals("নেটওয়ার্ক সমস্যা", errorState.errors["LOAD"])
        }
    }

    @Test
    fun `refresh passes refresh=true to the use case and reloads`() = runTest {
        val repo = FakeQbCurriculumRepository(subjectsResult = ApiResponse.Success(emptyList()))
        val viewModel = SubjectListViewModel(FetchSubjectsUseCase(repo))

        viewModel.state.test {
            skipItems(2) // initial loading + loaded, from init
            viewModel.refresh()
            skipItems(1) // loading again
            awaitItem()
        }
        assertEquals(true, repo.lastRefreshValue)
        assertEquals(2, repo.observeSubjectsCallCount)
    }

    @Test
    fun `clearError removes only the given tag`() = runTest {
        val repo = FakeQbCurriculumRepository(subjectsResult = ApiResponse.Failure("err", "LOAD_FAIL"))
        val viewModel = SubjectListViewModel(FetchSubjectsUseCase(repo))

        viewModel.state.test {
            skipItems(1)
            val errorState = awaitItem()
            assertTrue(errorState.errors.containsKey("LOAD"))
        }

        viewModel.clearError("LOAD")
        assertTrue(viewModel.state.value.errors.isEmpty())
    }
}
