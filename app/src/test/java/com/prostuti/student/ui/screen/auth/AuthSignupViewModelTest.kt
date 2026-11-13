package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.prostuti.student.R
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.data.repository.fake.fakeSignUpResponse
import com.prostuti.student.domain.PasswordBasedSignUpUseCase
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
class AuthSignupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeAuthRepository()
    private fun viewModel(identifier: String = "test@example.com") =
        AuthSignupViewModel(PasswordBasedSignUpUseCase(fakeRepository), identifier)

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
        assertEquals("", state.firstNameTextFieldState.text.toString())
        assertEquals("", state.lastNameTextFieldState.text.toString())
        assertEquals("", state.passwordTextFieldState.text.toString())
        assertFalse(state.isSignedUp)
    }

    @Test
    fun signUpWithShortNamesSetsErrorsAndDoesNotCallUseCase() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.firstNameTextFieldState.setTextAndPlaceCursorAtEnd("A")
        viewModel.state.value.lastNameTextFieldState.setTextAndPlaceCursorAtEnd("B")
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("Password1")
        viewModel.signUp()
        assertNotNull(viewModel.state.value.firstNameError)
        assertNotNull(viewModel.state.value.lastNameError)
        assertNull(fakeRepository.lastSignUpRequest)
    }

    @Test
    fun signUpWithPasswordMissingUppercaseSetsPasswordError() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.firstNameTextFieldState.setTextAndPlaceCursorAtEnd("Rahim")
        viewModel.state.value.lastNameTextFieldState.setTextAndPlaceCursorAtEnd("Islam")
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("password1")
        viewModel.signUp()
        assertEquals(R.string.error_password_needs_uppercase, viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastSignUpRequest)
    }

    @Test
    fun signUpWithPasswordMissingDigitSetsPasswordError() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.firstNameTextFieldState.setTextAndPlaceCursorAtEnd("Rahim")
        viewModel.state.value.lastNameTextFieldState.setTextAndPlaceCursorAtEnd("Islam")
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("Passwordonly")
        viewModel.signUp()
        assertNotNull(viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastSignUpRequest)
    }

    @Test
    fun signUpWithShortPasswordSetsPasswordError() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.firstNameTextFieldState.setTextAndPlaceCursorAtEnd("Rahim")
        viewModel.state.value.lastNameTextFieldState.setTextAndPlaceCursorAtEnd("Islam")
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("pass1")
        viewModel.signUp()
        assertNotNull(viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastSignUpRequest)
    }

    @Test
    fun signUpSuccessUpdatesIsSignedUpAndDerivesUserName() = runTest {
        fakeRepository.signUpResult = ApiResponse.Success(
            fakeSignUpResponse(username = "rahimislam")
        )
        val viewModel = viewModel(identifier = "test@example.com")
        viewModel.state.value.firstNameTextFieldState.setTextAndPlaceCursorAtEnd("Rahim")
        viewModel.state.value.lastNameTextFieldState.setTextAndPlaceCursorAtEnd("Islam")
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("Password1")

        viewModel.state.test {
            assertFalse(awaitItem().isSignedUp)
            viewModel.signUp()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertFalse(result.isLoading)
            assertTrue(result.isSignedUp)
        }
        assertEquals("rahimislam", fakeRepository.lastSignUpRequest?.userName)
        assertEquals("Rahim", fakeRepository.lastSignUpRequest?.firstName)
        assertEquals("Islam", fakeRepository.lastSignUpRequest?.lastName)
    }

    @Test
    fun signUpFailurePopulatesErrorsMap() = runTest {
        fakeRepository.signUpResult = ApiResponse.Failure(message = "Username taken", errorCode = "DUPLICATE")
        val viewModel = viewModel()
        viewModel.state.value.firstNameTextFieldState.setTextAndPlaceCursorAtEnd("Rahim")
        viewModel.state.value.lastNameTextFieldState.setTextAndPlaceCursorAtEnd("Islam")
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("Password1")

        viewModel.state.test {
            assertFalse(awaitItem().isSignedUp)
            viewModel.signUp()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertEquals("Username taken", result.errors["PASSWORD_BASED_SIGN_UP"])
        }
    }
}
