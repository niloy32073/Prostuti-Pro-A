package com.prostuti.student.ui.screen.auth

import app.cash.turbine.test
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.data.repository.fake.fakeLogInResponse
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
    private fun viewModel(resetToken: String = "reset-token") =
        AuthResetNewPasswordViewModel(ResetPasswordUseCase(fakeRepository), resetToken)

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
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isReset)
    }

    @Test
    fun resetWithShortPasswordSetsErrorAndDoesNotCallUseCase() = runTest {
        val viewModel = viewModel()
        viewModel.updatePassword("short1")
        viewModel.updateConfirmPassword("short1")
        viewModel.resetPassword()
        assertNotNull(viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastResetPasswordRequest)
    }

    @Test
    fun resetWithPasswordMissingDigitSetsError() = runTest {
        val viewModel = viewModel()
        viewModel.updatePassword("passwordonly")
        viewModel.updateConfirmPassword("passwordonly")
        viewModel.resetPassword()
        assertNotNull(viewModel.state.value.passwordError)
        assertNull(fakeRepository.lastResetPasswordRequest)
    }

    @Test
    fun resetWithMismatchedConfirmPasswordSetsError() = runTest {
        val viewModel = viewModel()
        viewModel.updatePassword("password1")
        viewModel.updateConfirmPassword("password2")
        viewModel.resetPassword()
        assertNotNull(viewModel.state.value.confirmPasswordError)
        assertNull(fakeRepository.lastResetPasswordRequest)
    }

    @Test
    fun resetWithEmptyConfirmPasswordSetsError() = runTest {
        val viewModel = viewModel()
        viewModel.updatePassword("password1")
        viewModel.resetPassword()
        assertNotNull(viewModel.state.value.confirmPasswordError)
        assertNull(fakeRepository.lastResetPasswordRequest)
    }

    @Test
    fun resetSuccessSetsIsResetAndUsesGivenToken() = runTest {
        fakeRepository.resetPasswordResult = ApiResponse.Success(fakeLogInResponse())
        val viewModel = viewModel(resetToken = "token-abc")
        viewModel.updatePassword("password1")
        viewModel.updateConfirmPassword("password1")

        viewModel.state.test {
            assertFalse(awaitItem().isReset)
            viewModel.resetPassword()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertFalse(result.isLoading)
            assertTrue(result.isReset)
        }
        assertEquals("token-abc", fakeRepository.lastResetPasswordRequest?.resetToken)
        assertEquals("password1", fakeRepository.lastResetPasswordRequest?.newPassword)
    }

    @Test
    fun resetFailurePopulatesErrorsMap() = runTest {
        fakeRepository.resetPasswordResult = ApiResponse.Failure(message = "Token expired", errorCode = "EXPIRED")
        val viewModel = viewModel()
        viewModel.updatePassword("password1")
        viewModel.updateConfirmPassword("password1")

        viewModel.state.test {
            assertFalse(awaitItem().isReset)
            viewModel.resetPassword()
            assertTrue(awaitItem().isLoading)
            val result = awaitItem()
            assertEquals("Token expired", result.errors["RESET_PASSWORD"])
        }
    }
}
