package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.data.repository.fake.fakeLogInResponse
import com.prostuti.student.domain.PasswordBasedSignInUseCase
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
class AuthLoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeAuthRepository()
    private fun viewModel(identifier: String = "test@example.com", firstName: String? = "Rahim") =
        AuthLoginViewModel(PasswordBasedSignInUseCase(fakeRepository), identifier, firstName)

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
        assertEquals("", state.passwordTextFieldState.text.toString())
        assertNull(state.passwordError)
        assertFalse(state.isSignedIn)
    }

    @Test
    fun signInWithShortPasswordSetsErrorAndDoesNotCallUseCase() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("123")
        viewModel.signIn()
        assertNotNull(viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastSignInRequest)
    }

    @Test
    fun signInSuccessUpdatesIsSignedIn() = runTest {
        fakeRepository.signInResult = ApiResponse.Success(fakeLogInResponse())
        val viewModel = viewModel(identifier = "test@example.com")
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("password123")

        viewModel.state.test {
            assertFalse(awaitItem().isSignedIn)
            viewModel.signIn()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertFalse(result.isLoading)
            assertTrue(result.isSignedIn)
        }
        assertEquals("test@example.com", fakeRepository.lastSignInRequest?.identifier)
        assertEquals("password123", fakeRepository.lastSignInRequest?.password)
    }

    @Test
    fun signInFailurePopulatesErrorsMap() = runTest {
        fakeRepository.signInResult = ApiResponse.Failure(message = "Wrong password", errorCode = "AUTH_FAILED")
        val viewModel = viewModel()
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("password123")

        viewModel.state.test {
            assertFalse(awaitItem().isSignedIn)
            viewModel.signIn()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertFalse(result.isLoading)
            assertFalse(result.isSignedIn)
            assertEquals("Wrong password", result.errors["PASSWORD_BASED_SIGN_IN"])
        }
    }

    @Test
    fun clearErrorRemovesTheTag() = runTest {
        fakeRepository.signInResult = ApiResponse.Failure(message = "err", errorCode = "AUTH_FAILED")
        val viewModel = viewModel()
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("password123")
        viewModel.signIn()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.errors.containsKey("PASSWORD_BASED_SIGN_IN"))

        viewModel.clearError("PASSWORD_BASED_SIGN_IN")
        assertFalse(viewModel.state.value.errors.containsKey("PASSWORD_BASED_SIGN_IN"))
    }
}
