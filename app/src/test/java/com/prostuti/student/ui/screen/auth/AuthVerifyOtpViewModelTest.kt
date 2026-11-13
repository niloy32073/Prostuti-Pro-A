package com.prostuti.student.ui.screen.auth

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.prostuti.student.data.model.enums.OTPVerificationPurpose
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.VerifyEmailResponse
import com.prostuti.student.data.model.response.VerifyPhoneResponse
import com.prostuti.student.data.model.enums.UserStatus
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.domain.SendOTPUseCase
import com.prostuti.student.domain.VerifyEmailUseCase
import com.prostuti.student.domain.VerifyPhoneUseCase
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
class AuthVerifyOtpViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeAuthRepository()
    private fun viewModel(identifier: String) = AuthVerifyOtpViewModel(
        SendOTPUseCase(fakeRepository), VerifyEmailUseCase(fakeRepository), VerifyPhoneUseCase(fakeRepository), identifier
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun creatingWithEmailIdentifierSendsEmailVerifyOtp() = runTest {
        viewModel("test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepository.sendOtpCallCount)
        assertEquals(OTPVerificationPurpose.EMAIL_VERIFY, fakeRepository.lastSendOtpRequest?.purpose)
    }

    @Test
    fun creatingWithPhoneIdentifierSendsPhoneVerifyOtp() = runTest {
        viewModel("01712345678")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepository.sendOtpCallCount)
        assertEquals(OTPVerificationPurpose.PHONE_VERIFY, fakeRepository.lastSendOtpRequest?.purpose)
    }

    @Test
    fun countdownDecreasesAfterOtpSentSuccessfully() = runTest {
        val viewModel = viewModel("test@example.com")
        testDispatcher.scheduler.advanceTimeBy(1_500)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.state.value.secondsLeft < 28)
    }

    @Test
    fun verifyWithShortCodeSetsErrorAndDoesNotCallUseCase() = runTest {
        val viewModel = viewModel("test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.state.value.codeTextFieldState.setTextAndPlaceCursorAtEnd("123")
        viewModel.verify()
        assertNotNull(viewModel.state.value.codeError)
        assertNull(fakeRepository.lastVerifyEmailRequest)
    }

    @Test
    fun verifySuccessForEmailIdentifierUsesVerifyEmailAndSetsVerified() = runTest {
        fakeRepository.verifyEmailResult = ApiResponse.Success(VerifyEmailResponse(UserStatus.ACTIVE, isEmailVerified = true))
        val viewModel = viewModel("test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.state.value.codeTextFieldState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.verify()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isVerified)
        assertEquals("123456", fakeRepository.lastVerifyEmailRequest?.code)
        assertNull(fakeRepository.lastVerifyPhoneRequest)
    }

    @Test
    fun verifySuccessForPhoneIdentifierUsesVerifyPhone() = runTest {
        fakeRepository.verifyPhoneResult = ApiResponse.Success(VerifyPhoneResponse(UserStatus.ACTIVE, isPhoneVerified = true))
        val viewModel = viewModel("01712345678")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.state.value.codeTextFieldState.setTextAndPlaceCursorAtEnd("123456")
        viewModel.verify()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isVerified)
        assertEquals("123456", fakeRepository.lastVerifyPhoneRequest?.code)
        assertNull(fakeRepository.lastVerifyEmailRequest)
    }

    @Test
    fun verifyFailurePopulatesErrorsMap() = runTest {
        fakeRepository.verifyEmailResult = ApiResponse.Failure(message = "Invalid code", errorCode = "BAD_OTP")
        val viewModel = viewModel("test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.state.value.codeTextFieldState.setTextAndPlaceCursorAtEnd("000000")
        viewModel.verify()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isVerified)
        assertEquals("Invalid code", viewModel.state.value.errors["VERIFY_OTP"])
    }

    @Test
    fun resendWhileCooldownActiveDoesNotCallSendOtpAgain() = runTest {
        val viewModel = viewModel("test@example.com")
        testDispatcher.scheduler.advanceTimeBy(1_500) // OTP sent, countdown still running (secondsLeft ~26-27)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, fakeRepository.sendOtpCallCount)

        viewModel.resend()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepository.sendOtpCallCount) // still cooling down, resend() was a no-op
    }

    @Test
    fun resendAfterCooldownElapsedCallsSendOtpAgain() = runTest {
        val viewModel = viewModel("test@example.com")
        testDispatcher.scheduler.advanceUntilIdle() // initial send + full 28s countdown runs to completion
        assertEquals(1, fakeRepository.sendOtpCallCount)

        viewModel.resend()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, fakeRepository.sendOtpCallCount)
    }
}
