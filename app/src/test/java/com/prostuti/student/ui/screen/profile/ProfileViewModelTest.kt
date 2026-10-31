package com.prostuti.student.ui.screen.profile

import app.cash.turbine.test
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.domain.GetMeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val me = FakeUserRepository.me()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun `opening reads the cache and shows the profile`() = runTest(testDispatcher) {
        val repo = FakeUserRepository(listOf(ApiResponse.Success(me)))
        val viewModel = ProfileViewModel(GetMeUseCase(repo))

        viewModel.state.test {
            assertEquals(ProfileUiState(), awaitItem())
            assertTrue(awaitItem().isLoading)
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(me, loaded.profile)
            assertTrue(loaded.errors.isEmpty())
        }
        assertEquals(listOf(false), repo.refreshArgs)
    }

    @Test fun `refresh goes to the network`() = runTest(testDispatcher) {
        val repo = FakeUserRepository(listOf(ApiResponse.Success(me)))
        val viewModel = ProfileViewModel(GetMeUseCase(repo))
        advanceUntilIdle()

        repo.results = listOf(ApiResponse.Success(me.copy(firstName = "Updated")))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(listOf(false, true), repo.refreshArgs)
        assertEquals("Updated", viewModel.state.value.profile?.firstName)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test fun `failure with no cache shows the error and stops loading`() = runTest(testDispatcher) {
        val repo = FakeUserRepository(listOf(ApiResponse.Failure(message = "No internet connection", errorCode = "NETWORK_ERROR")))
        val viewModel = ProfileViewModel(GetMeUseCase(repo))

        viewModel.state.test {
            skipItems(2) // initial, loading
            val failed = awaitItem()
            assertFalse(failed.isLoading)
            assertNull(failed.profile)
            assertEquals("No internet connection", failed.errors[ProfileViewModel.ERR_LOAD])
        }
    }

    @Test fun `failed refresh keeps the cached profile and reports the error`() = runTest(testDispatcher) {
        val repo = FakeUserRepository(
            listOf(ApiResponse.Failure(message = "Timeout", errorCode = "TIMEOUT_ERROR"), ApiResponse.Success(me))
        )
        val viewModel = ProfileViewModel(GetMeUseCase(repo))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(me, state.profile)
        assertEquals("Timeout", state.errors[ProfileViewModel.ERR_LOAD])
        assertFalse(state.isLoading)
    }

    @Test fun `clearError drops the tag`() = runTest(testDispatcher) {
        val repo = FakeUserRepository(listOf(ApiResponse.Failure(message = "x", errorCode = "E")))
        val viewModel = ProfileViewModel(GetMeUseCase(repo))
        advanceUntilIdle()

        viewModel.clearError(ProfileViewModel.ERR_LOAD)

        assertTrue(viewModel.state.value.errors.isEmpty())
    }
}
