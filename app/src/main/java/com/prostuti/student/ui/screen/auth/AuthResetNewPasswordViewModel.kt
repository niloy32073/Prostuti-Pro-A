package com.prostuti.student.ui.screen.auth

import androidx.annotation.StringRes
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
    val password: String = "",
    val confirmPassword: String = "",
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isReset: Boolean = false
)

private const val ERR_RESET = "RESET_PASSWORD"
private val PASSWORD_HAS_DIGIT = Regex(".*\\d.*")

class AuthResetNewPasswordViewModel(
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val resetToken: String
) : ViewModel() {
    private val _state = MutableStateFlow(AuthResetNewPasswordUiState())
    val state: StateFlow<AuthResetNewPasswordUiState> = _state.asStateFlow()

    fun updatePassword(value: String) = _state.update { it.copy(password = value, passwordError = null) }
    fun updateConfirmPassword(value: String) = _state.update { it.copy(confirmPassword = value, confirmPasswordError = null) }

    private fun validate(): Boolean {
        val s = _state.value
        val passwordError = when {
            s.password.length < 8 -> R.string.error_password_min_8
            !s.password.matches(PASSWORD_HAS_DIGIT) -> R.string.error_password_needs_digit
            else -> null
        }
        val confirmError = when {
            s.confirmPassword.isEmpty() -> R.string.error_confirm_password_required
            s.confirmPassword != s.password -> R.string.error_passwords_dont_match
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
                ResetPasswordRequest(resetToken = resetToken, newPassword = _state.value.password)
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
