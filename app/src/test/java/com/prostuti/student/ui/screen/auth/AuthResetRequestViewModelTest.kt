package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.prostuti.student.data.model.enums.OTPVerificationPurpose
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.domain.SendOTPUseCase
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
class AuthResetRequestViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeAuthRepository()
    private fun viewModel(identifier: String = "test@example.com") =
        AuthResetRequestViewModel(SendOTPUseCase(fakeRepository), identifier)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateHasNoCodeSent() {
        val state = viewModel().state.value
        assertFalse(state.isCodeSent)
        assertNull(state.enteredCode)
    }

    @Test
    fun sendCodeSuccessMarksCodeSentAndStartsCountdown() = runTest {
        val viewModel = viewModel("test@example.com")
        viewModel.sendCode()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isCodeSent)
        assertEquals(OTPVerificationPurpose.PASSWORD_RESET, fakeRepository.lastSendOtpRequest?.purpose)
        assertEquals("test@example.com", fakeRepository.lastSendOtpRequest?.identifier)
    }

    @Test
    fun sendCodeFailurePopulatesErrorsMap() = runTest {
        fakeRepository.sendOtpResult = ApiResponse.Failure(message = "Too many requests", errorCode = "RATE_LIMIT")
        val viewModel = viewModel()
        viewModel.sendCode()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isCodeSent)
        assertEquals("Too many requests", viewModel.state.value.errors["SEND_RESET_OTP"])
    }

    @Test
    fun verifyWithShortCodeSetsErrorAndDoesNotMoveOn() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.codeTextFieldState.setTextAndPlaceCursorAtEnd("12")
        viewModel.verify()
        assertNotNull(viewModel.state.value.codeError)
        assertNull(viewModel.state.value.enteredCode)
    }

    @Test
    fun verifyWithSixDigitsHandsTheCodeOnWithoutCallingTheServer() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.codeTextFieldState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.verify()

        assertEquals("123456", viewModel.state.value.enteredCode)
        assertNull(fakeRepository.lastResetPasswordRequest) // checked with the new password, not here
    }

    @Test
    fun onCodeConsumedClearsTheEnteredCode() = runTest {
        val viewModel = viewModel()
        viewModel.state.value.codeTextFieldState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.verify()
        viewModel.onCodeConsumed()
        assertNull(viewModel.state.value.enteredCode)
    }

    @Test
    fun resendAfterCooldownElapsedCallsSendOtpAgain() = runTest {
        val viewModel = viewModel()
        viewModel.sendCode()
        testDispatcher.scheduler.advanceUntilIdle() // send + full countdown
        assertEquals(1, fakeRepository.sendOtpCallCount)

        viewModel.resend()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, fakeRepository.sendOtpCallCount)
    }
}
