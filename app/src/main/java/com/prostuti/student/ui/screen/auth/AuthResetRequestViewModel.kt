package com.prostuti.student.ui.screen.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.OTPVerificationPurpose
import com.prostuti.student.data.model.request.SendOTPRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.SendOTPUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN_SECONDS = 28
private const val ERR_SEND_OTP = "SEND_RESET_OTP"

data class AuthResetRequestUiState(
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    /** False = "send code" step; true = code sent, the inline OTP step is shown. */
    val isCodeSent: Boolean = false,
    val codeTextFieldState: TextFieldState = TextFieldState(""),
    @StringRes val codeError: Int? = null,
    val secondsLeft: Int = RESEND_COOLDOWN_SECONDS,
    /** The 6-digit code, set once entered; the screen moves on with it, then [AuthResetRequestViewModel.onCodeConsumed]. */
    val enteredCode: String? = null
)

/**
 * Handles both the "send a reset code" step and, inline on the same screen once the code is
 * sent, the "verify it" step — no separate OTP screen for password reset (unlike signup's
 * [AuthVerifyOtpViewModel], which is a distinct flow and stays a separate screen).
 */
class AuthResetRequestViewModel(
    private val sendOTPUseCase: SendOTPUseCase,
    val identifier: String
) : ViewModel() {
    private val _state = MutableStateFlow(AuthResetRequestUiState())
    val state: StateFlow<AuthResetRequestUiState> = _state.asStateFlow()
    private var countdownJob: Job? = null

    init {
        val code = _state.value.codeTextFieldState
        viewModelScope.launch {
            snapshotFlow { code.text.toString() }.collect { _state.update { it.copy(codeError = null) } }
        }
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

    /**
     * Checks only the code's shape: the server has no separate verify step for a reset OTP and
     * checks it together with the new password on the next screen.
     */
    fun verify() {
        val code = _state.value.codeTextFieldState.text.toString().trim()
        if (code.length != 6) {
            _state.update { it.copy(codeError = R.string.error_otp_required) }
            return
        }
        _state.update { it.copy(enteredCode = code) }
    }

    fun onCodeConsumed() {
        _state.update { it.copy(enteredCode = null) }
    }

    fun clearError(tag: String) {
        _state.update { it.copy(errors = it.errors - tag) }
    }
}
