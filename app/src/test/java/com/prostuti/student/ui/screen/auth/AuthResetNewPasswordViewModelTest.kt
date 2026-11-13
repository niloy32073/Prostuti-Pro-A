package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.prostuti.student.R
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.domain.ResetPasswordUseCase
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
class AuthResetNewPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeAuthRepository()
    private fun viewModel(identifier: String = "test@example.com", code: String = "123456") =
        AuthResetNewPasswordViewModel(ResetPasswordUseCase(fakeRepository), identifier, code)

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
        assertEquals("", state.confirmPasswordTextFieldState.text.toString())
        assertFalse(state.isReset)
    }

    @Test
    fun resetWithShortPasswordSetsErrorAndDoesNotCallUseCase() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("short1")
        viewModel.state.value.confirmPasswordTextFieldState.setTextAndPlaceCursorAtEnd("short1")
        viewModel.resetPassword()
        assertNotNull(viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastResetPasswordRequest)
    }

    @Test
    fun resetWithPasswordMissingUppercaseSetsError() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("password1")
        viewModel.state.value.confirmPasswordTextFieldState.setTextAndPlaceCursorAtEnd("password1")
        viewModel.resetPassword()
        assertEquals(R.string.error_password_needs_uppercase, viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastResetPasswordRequest)
    }

    @Test
    fun resetWithPasswordMissingDigitSetsError() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("Passwordonly")
        viewModel.state.value.confirmPasswordTextFieldState.setTextAndPlaceCursorAtEnd("Passwordonly")
        viewModel.resetPassword()
        assertNotNull(viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastResetPasswordRequest)
    }

    @Test
    fun resetWithMismatchedConfirmPasswordSetsError() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("Password1")
        viewModel.state.value.confirmPasswordTextFieldState.setTextAndPlaceCursorAtEnd("Password2")
        viewModel.resetPassword()
        assertNotNull(viewModel.state.value.confirmPasswordError)
        assertNull(fakeRepository.lastResetPasswordRequest)
    }

    @Test
    fun resetWithEmptyConfirmPasswordSetsError() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("Password1")
        viewModel.resetPassword()
        assertNotNull(viewModel.state.value.confirmPasswordError)
        assertNull(fakeRepository.lastResetPasswordRequest)
    }

    @Test
    fun resetSuccessSetsIsResetAndSendsIdentifierCodeAndPassword() = runTest {
        fakeRepository.resetPasswordResult = ApiResponse.Success(Unit)
        val viewModel = viewModel(identifier = "rahim@example.com", code = "654321")
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("Password1")
        viewModel.state.value.confirmPasswordTextFieldState.setTextAndPlaceCursorAtEnd("Password1")

        viewModel.state.test {
            assertFalse(awaitItem().isReset)
            viewModel.resetPassword()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertFalse(result.isLoading)
            assertTrue(result.isReset)
        }
        assertEquals("rahim@example.com", fakeRepository.lastResetPasswordRequest?.identifier)
        assertEquals("654321", fakeRepository.lastResetPasswordRequest?.code)
        assertEquals("Password1", fakeRepository.lastResetPasswordRequest?.newPassword)
    }

    @Test
    fun resetFailurePopulatesErrorsMap() = runTest {
        fakeRepository.resetPasswordResult = ApiResponse.Failure(message = "Invalid code", errorCode = "OTP_INVALID")
        val viewModel = viewModel()
        viewModel.state.value.passwordTextFieldState.setTextAndPlaceCursorAtEnd("Password1")
        viewModel.state.value.confirmPasswordTextFieldState.setTextAndPlaceCursorAtEnd("Password1")

        viewModel.state.test {
            assertFalse(awaitItem().isReset)
            viewModel.resetPassword()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertEquals("Invalid code", result.errors["RESET_PASSWORD"])
        }
    }
}
