package com.prostuti.student.ui.screen.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.request.ResetPasswordRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.ResetPasswordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthResetNewPasswordUiState(
    val passwordTextFieldState: TextFieldState = TextFieldState(""),
    val confirmPasswordTextFieldState: TextFieldState = TextFieldState(""),
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isReset: Boolean = false
)

private const val ERR_RESET = "RESET_PASSWORD"
private val PASSWORD_HAS_DIGIT = Regex(".*\\d.*")
// The server rejects a password without an uppercase letter (RegisterRequest / ResetPasswordRequest).
private val PASSWORD_HAS_UPPERCASE = Regex(".*[A-Z].*")

class AuthResetNewPasswordViewModel(
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val identifier: String,
    private val code: String
) : ViewModel() {
    private val _state = MutableStateFlow(AuthResetNewPasswordUiState())
    val state: StateFlow<AuthResetNewPasswordUiState> = _state.asStateFlow()

    init {
        val password = _state.value.passwordTextFieldState
        val confirmPassword = _state.value.confirmPasswordTextFieldState
        viewModelScope.launch {
            snapshotFlow { password.text.toString() }.collect { _state.update { it.copy(passwordError = null) } }
        }
        viewModelScope.launch {
            snapshotFlow { confirmPassword.text.toString() }.collect { _state.update { it.copy(confirmPasswordError = null) } }
        }
    }

    private fun validate(): Boolean {
        val s = _state.value
        val passwordError = when {
            s.passwordTextFieldState.text.length < 8 -> R.string.error_password_min_8
            !s.passwordTextFieldState.text.toString().matches(PASSWORD_HAS_DIGIT) -> R.string.error_password_needs_digit
            !s.passwordTextFieldState.text.toString().matches(PASSWORD_HAS_UPPERCASE) -> R.string.error_password_needs_uppercase
            else -> null
        }
        val confirmError = when {
            s.confirmPasswordTextFieldState.text.toString().isEmpty() -> R.string.error_confirm_password_required
            s.confirmPasswordTextFieldState.text.toString() != s.passwordTextFieldState.text.toString() -> R.string.error_passwords_dont_match
            else -> null
        }
        _state.update { it.copy(passwordError = passwordError, confirmPasswordError = confirmError) }
        return passwordError == null && confirmError == null
    }

    fun resetPassword() {
        if (!validate()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errors = it.errors - ERR_RESET) }
            val response = resetPasswordUseCase(
                ResetPasswordRequest(
                    identifier = identifier,
                    code = code,
                    newPassword = _state.value.passwordTextFieldState.text.toString()
                )
            )
            when (response) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, isReset = true) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_RESET to response.message))
                }
            }
        }
    }

    fun clearError(tag: String) {
        _state.update { it.copy(errors = it.errors - tag) }
    }
}
