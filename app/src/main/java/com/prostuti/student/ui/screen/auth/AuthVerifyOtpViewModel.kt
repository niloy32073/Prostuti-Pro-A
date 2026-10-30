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
import com.prostuti.student.domain.VerifyEmailUseCase
import com.prostuti.student.domain.VerifyPhoneUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN_SECONDS = 28
private const val ERR_SEND_OTP = "SEND_OTP"
private const val ERR_VERIFY_OTP = "VERIFY_OTP"

data class AuthVerifyOtpUiState(
    val code: String = "",
    @StringRes val codeError: Int? = null,
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val secondsLeft: Int = RESEND_COOLDOWN_SECONDS,
    val isVerified: Boolean = false
)

class AuthVerifyOtpViewModel(
    private val sendOTPUseCase: SendOTPUseCase,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val verifyPhoneUseCase: VerifyPhoneUseCase,
    val identifier: String
) : ViewModel() {
    private val _state = MutableStateFlow(AuthVerifyOtpUiState())
    val state: StateFlow<AuthVerifyOtpUiState> = _state.asStateFlow()
    private val isEmail = identifier.any { it.isLetter() }
    private var countdownJob: Job? = null

    init {
        sendOtp()
    }

    fun updateCode(value: String) {
        _state.update { it.copy(code = value, codeError = null) }
    }

    fun resend() {
        if (_state.value.secondsLeft > 0) return
        sendOtp()
    }

    private fun sendOtp() {
        viewModelScope.launch {
            _state.update { it.copy(errors = it.errors - ERR_SEND_OTP) }
            val purpose = if (isEmail) OTPVerificationPurpose.EMAIL_VERIFY else OTPVerificationPurpose.PHONE_VERIFY
            when (val response = sendOTPUseCase(SendOTPRequest(identifier, purpose))) {
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
            val request = VerifyIdentifierRequest(identifier, code)
            val response = if (isEmail) verifyEmailUseCase(request) else verifyPhoneUseCase(request)
            val failureMessage = when (response) {
                is ApiResponse.Success<*> -> null
                is ApiResponse.Failure -> response.message
            }
            if (failureMessage == null) {
                _state.update { it.copy(isLoading = false, isVerified = true) }
            } else {
                _state.update { it.copy(isLoading = false, errors = it.errors + (ERR_VERIFY_OTP to failureMessage)) }
            }
        }
    }

    fun clearError(tag: String) {
        _state.update { it.copy(errors = it.errors - tag) }
    }
}
