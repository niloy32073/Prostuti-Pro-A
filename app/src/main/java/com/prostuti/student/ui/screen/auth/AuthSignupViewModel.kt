package com.prostuti.student.ui.screen.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.request.PasswordBasedSignUpRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.PasswordBasedSignUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthSignupUiState(
    val firstNameTextFieldState: TextFieldState = TextFieldState(""),
    val lastNameTextFieldState: TextFieldState = TextFieldState(""),
    val passwordTextFieldState: TextFieldState = TextFieldState(""),
    @StringRes val firstNameError: Int? = null,
    @StringRes val lastNameError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isSignedUp: Boolean = false
)

private const val ERR_SIGN_UP = "PASSWORD_BASED_SIGN_UP"
private val PASSWORD_HAS_DIGIT = Regex(".*\\d.*")
// The server rejects a password without an uppercase letter (RegisterRequest / ResetPasswordRequest).
private val PASSWORD_HAS_UPPERCASE = Regex(".*[A-Z].*")

class AuthSignupViewModel(
    private val passwordBasedSignUpUseCase: PasswordBasedSignUpUseCase,
    val identifier: String
) : ViewModel() {
    private val _state = MutableStateFlow(AuthSignupUiState())
    val state: StateFlow<AuthSignupUiState> = _state.asStateFlow()

    init {
        val password = _state.value.passwordTextFieldState
        val firstName = _state.value.firstNameTextFieldState
        val lastName = _state.value.lastNameTextFieldState
        viewModelScope.launch {
            snapshotFlow { password.text.toString() }.collect { _state.update { it.copy(passwordError = null) } }
        }
        viewModelScope.launch {
            snapshotFlow { firstName.text.toString() }.collect { _state.update { it.copy(firstNameError = null) } }
        }
        viewModelScope.launch {
            snapshotFlow { lastName.text.toString() }.collect { _state.update { it.copy(lastNameError = null) } }
        }
    }

    private fun validate(): Boolean {
        val s = _state.value
        val firstName = s.firstNameTextFieldState.text.toString().trim()
        val lastName = s.lastNameTextFieldState.text.toString().trim()
        val firstNameError = if (firstName.length < 2) R.string.error_name_min_2 else null
        val lastNameError = if (lastName.length < 2) R.string.error_name_min_2 else null
        val passwordError = when {
            s.passwordTextFieldState.text.length < 8 -> R.string.error_password_min_8
            !s.passwordTextFieldState.text.toString().matches(PASSWORD_HAS_DIGIT) -> R.string.error_password_needs_digit
            !s.passwordTextFieldState.text.toString().matches(PASSWORD_HAS_UPPERCASE) -> R.string.error_password_needs_uppercase
            else -> null
        }
        _state.update { it.copy(firstNameError = firstNameError, lastNameError = lastNameError, passwordError = passwordError) }
        return firstNameError == null && lastNameError == null && passwordError == null
    }

    fun signUp() {
        if (!validate()) return
        viewModelScope.launch {
            val s = _state.value
            val firstName = s.firstNameTextFieldState.text.toString().trim()
            val lastName = s.lastNameTextFieldState.text.toString().trim()
            val userName = (firstName + lastName).lowercase().filter { it.isLetterOrDigit() }
            _state.update { it.copy(isLoading = true, errors = it.errors - ERR_SIGN_UP) }
            val response = passwordBasedSignUpUseCase(
                PasswordBasedSignUpRequest(
                    identifier = identifier,
                    password = s.passwordTextFieldState.text.toString(),
                    firstName = firstName,
                    lastName = lastName,
                    userName = userName
                )
            )
            when (response) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, isSignedUp = true) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_SIGN_UP to response.message))
                }
            }
        }
    }

    fun clearError(tag: String) {
        _state.update { it.copy(errors = it.errors - tag) }
    }
}
