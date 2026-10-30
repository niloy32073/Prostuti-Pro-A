package com.prostuti.student.ui.screen.auth

import app.cash.turbine.test
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CheckUserResponse
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.domain.CheckUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class AuthStartViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeAuthRepository()
    private fun viewModel() = AuthStartViewModel(CheckUserUseCase(fakeRepository))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsEmpty() {
        val state = viewModel().state.value
        assertEquals("", state.identifier)
        assertNull(state.identifierError)
        assertFalse(state.isLoading)
        assertNull(state.accountExists)
    }

    @Test
    fun continueClickedWithEmptyIdentifierSetsErrorAndDoesNotCallUseCase() = runTest {
        val viewModel = viewModel()
        viewModel.continueClicked()
        assertNotNull(viewModel.state.value.identifierError)
        assertNull(fakeRepository.lastCheckUserRequest)
    }

    @Test
    fun continueClickedWithInvalidEmailSetsError() = runTest {
        val viewModel = viewModel()
        viewModel.updateIdentifier("not-an-email")
        viewModel.continueClicked()
        assertNotNull(viewModel.state.value.identifierError)
        assertNull(fakeRepository.lastCheckUserRequest)
    }

    @Test
    fun continueClickedWithValidEmailAndExistingAccountUpdatesState() = runTest {
        fakeRepository.checkUserResult = ApiResponse.Success(CheckUserResponse(exists = true, firstName = "Rahim"))
        val viewModel = viewModel()
        viewModel.updateIdentifier("test@example.com")

        viewModel.state.test {
            assertFalse(awaitItem().isLoading) // initial
            viewModel.continueClicked()
            assertTrue(awaitItem().isLoading) // loading
            val result = awaitItem()
            assertFalse(result.isLoading)
            assertEquals(true, result.accountExists)
            assertEquals("Rahim", result.firstName)
        }
        assertEquals("test@example.com", fakeRepository.lastCheckUserRequest?.identifier)
    }

    @Test
    fun continueClickedWithValidPhoneAndFailurePopulatesErrorsMap() = runTest {
        fakeRepository.checkUserResult = ApiResponse.Failure(message = "Server unavailable", errorCode = "SERVER_ERROR")
        val viewModel = viewModel()
        viewModel.updateIdentifier("01712345678")

        viewModel.state.test {
            assertFalse(awaitItem().isLoading)
            viewModel.continueClicked()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertFalse(result.isLoading)
            assertEquals("Server unavailable", result.errors["CHECK_USER"])
        }
    }

    @Test
    fun clearErrorRemovesTheTag() = runTest {
        fakeRepository.checkUserResult = ApiResponse.Failure(message = "err", errorCode = "SERVER_ERROR")
        val viewModel = viewModel()
        viewModel.updateIdentifier("test@example.com")
        viewModel.continueClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.errors.containsKey("CHECK_USER"))

        viewModel.clearError("CHECK_USER")
        assertFalse(viewModel.state.value.errors.containsKey("CHECK_USER"))
    }

    @Test
    fun consumeNavigationClearsAccountExists() = runTest {
        fakeRepository.checkUserResult = ApiResponse.Success(CheckUserResponse(exists = true, firstName = null))
        val viewModel = viewModel()
        viewModel.updateIdentifier("test@example.com")
        viewModel.continueClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.state.value.accountExists)

        viewModel.consumeNavigation()
        assertNull(viewModel.state.value.accountExists)
    }
}
