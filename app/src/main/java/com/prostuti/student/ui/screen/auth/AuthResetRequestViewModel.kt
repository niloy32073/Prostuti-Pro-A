package com.prostuti.student.ui.screen.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.OTPVerificationPurpose
import com.prostuti.student.data.model.request.SendOTPRequest
import com.prostuti.student.data.model.request.VerifyIdentifierRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.SendOTPUseCase
import com.prostuti.student.domain.VerifyResetOtpUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN_SECONDS = 28
private const val ERR_SEND_OTP = "SEND_RESET_OTP"
private const val ERR_VERIFY_OTP = "VERIFY_RESET_OTP"

data class AuthResetRequestUiState(
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    /** False = "send code" step; true = code sent, the inline OTP step is shown. */
    val isCodeSent: Boolean = false,
    val code: String = "",
    @StringRes val codeError: Int? = null,
    val secondsLeft: Int = RESEND_COOLDOWN_SECONDS,
    val resetToken: String? = null
)

/**
 * Handles both the "send a reset code" step and, inline on the same screen once the code is
 * sent, the "verify it" step — no separate OTP screen for password reset (unlike signup's
 * [AuthVerifyOtpViewModel], which is a distinct flow and stays a separate screen).
 */
class AuthResetRequestViewModel(
    private val sendOTPUseCase: SendOTPUseCase,
    private val verifyResetOtpUseCase: VerifyResetOtpUseCase,
    val identifier: String
) : ViewModel() {
    private val _state = MutableStateFlow(AuthResetRequestUiState())
    val state: StateFlow<AuthResetRequestUiState> = _state.asStateFlow()
    private var countdownJob: Job? = null

    fun updateCode(value: String) {
        _state.update { it.copy(code = value, codeError = null) }
    }

    fun sendCode() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errors = it.errors - ERR_SEND_OTP) }
            val response = sendOTPUseCase(SendOTPRequest(identifier, OTPVerificationPurpose.PASSWORD_RESET))
            when (response) {
                is ApiResponse.Success -> {
                    _state.update { it.copy(isLoading = false, isCodeSent = true) }
                    startCountdown()
                }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_SEND_OTP to response.message))
                }
            }
        }
    }

    fun resend() {
        if (_state.value.secondsLeft > 0) return
        viewModelScope.launch {
            _state.update { it.copy(errors = it.errors - ERR_SEND_OTP) }
            when (val response = sendOTPUseCase(SendOTPRequest(identifier, OTPVerificationPurpose.PASSWORD_RESET))) {
                is ApiResponse.Success -> startCountdown()
                is ApiResponse.Failure -> _state.update { it.copy(errors = it.errors + (ERR_SEND_OTP to response.message)) }
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        _state.update { it.copy(secondsLeft = RESEND_COOLDOWN_SECONDS) }
        countdownJob = viewModelScope.launch {
            while (_state.value.secondsLeft > 0) {
                delay(1000)
                _state.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
        }
    }

    fun verify() {
        val code = _state.value.code.trim()
        if (code.length != 6) {
            _state.update { it.copy(codeError = R.string.error_otp_required) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errors = it.errors - ERR_VERIFY_OTP) }
            when (val response = verifyResetOtpUseCase(VerifyIdentifierRequest(identifier, code))) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, resetToken = response.data.resetToken) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_VERIFY_OTP to response.message))
                }
            }
        }
    }

    fun clearError(tag: String) {
        _state.update { it.copy(errors = it.errors - tag) }
    }
}
