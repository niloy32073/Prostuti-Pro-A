package com.prostuti.student.ui.screen.auth

import com.prostuti.student.data.model.enums.OTPVerificationPurpose
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.ResetTokenResponse
import com.prostuti.student.data.repository.fake.FakeAuthRepository
import com.prostuti.student.domain.SendOTPUseCase
import com.prostuti.student.domain.VerifyResetOtpUseCase
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
        AuthResetRequestViewModel(SendOTPUseCase(fakeRepository), VerifyResetOtpUseCase(fakeRepository), identifier)

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
        assertNull(state.resetToken)
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
    fun verifyWithShortCodeSetsErrorAndDoesNotCallUseCase() = runTest {
        val viewModel = viewModel()
        viewModel.updateCode("12")
        viewModel.verify()
        assertNotNull(viewModel.state.value.codeError)
        assertNull(fakeRepository.lastVerifyResetOtpRequest)
    }

    @Test
    fun verifySuccessStoresResetToken() = runTest {
        fakeRepository.verifyResetOtpResult = ApiResponse.Success(ResetTokenResponse(resetToken = "token-xyz"))
        val viewModel = viewModel()
        viewModel.updateCode("123456")
        viewModel.verify()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("token-xyz", viewModel.state.value.resetToken)
    }

    @Test
    fun verifyFailurePopulatesErrorsMap() = runTest {
        fakeRepository.verifyResetOtpResult = ApiResponse.Failure(message = "Invalid code", errorCode = "BAD_OTP")
        val viewModel = viewModel()
        viewModel.updateCode("123456")
        viewModel.verify()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.resetToken)
        assertEquals("Invalid code", viewModel.state.value.errors["VERIFY_RESET_OTP"])
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
