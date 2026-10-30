package com.prostuti.student.ui.screen.auth

import androidx.annotation.StringRes
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
    val firstName: String = "",
    val lastName: String = "",
    val password: String = "",
    @StringRes val firstNameError: Int? = null,
    @StringRes val lastNameError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isSignedUp: Boolean = false
)

private const val ERR_SIGN_UP = "PASSWORD_BASED_SIGN_UP"
private val PASSWORD_HAS_DIGIT = Regex(".*\\d.*")

class AuthSignupViewModel(
    private val passwordBasedSignUpUseCase: PasswordBasedSignUpUseCase,
    val identifier: String
) : ViewModel() {
    private val _state = MutableStateFlow(AuthSignupUiState())
    val state: StateFlow<AuthSignupUiState> = _state.asStateFlow()

    fun updateFirstName(value: String) = _state.update { it.copy(firstName = value, firstNameError = null) }
    fun updateLastName(value: String) = _state.update { it.copy(lastName = value, lastNameError = null) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value, passwordError = null) }

    private fun validate(): Boolean {
        val s = _state.value
        val firstNameError = if (s.firstName.trim().length < 2) R.string.error_name_min_2 else null
        val lastNameError = if (s.lastName.trim().length < 2) R.string.error_name_min_2 else null
        val passwordError = when {
            s.password.length < 8 -> R.string.error_password_min_8
            !s.password.matches(PASSWORD_HAS_DIGIT) -> R.string.error_password_needs_digit
            else -> null
        }
        _state.update { it.copy(firstNameError = firstNameError, lastNameError = lastNameError, passwordError = passwordError) }
        return firstNameError == null && lastNameError == null && passwordError == null
    }

    fun signUp() {
        if (!validate()) return
        viewModelScope.launch {
            val s = _state.value
            val userName = (s.firstName + s.lastName).lowercase().filter { it.isLetterOrDigit() }
            _state.update { it.copy(isLoading = true, errors = it.errors - ERR_SIGN_UP) }
            val response = passwordBasedSignUpUseCase(
                PasswordBasedSignUpRequest(
                    identifier = identifier,
                    password = s.password,
                    firstName = s.firstName.trim(),
                    lastName = s.lastName.trim(),
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
