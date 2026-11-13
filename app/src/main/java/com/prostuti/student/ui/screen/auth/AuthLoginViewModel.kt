 package com.prostuti.student.ui.screen.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.enums.DeviceCategory
import com.prostuti.student.data.model.request.PasswordBasedSignInRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.PasswordBasedSignInUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthLoginUiState(
    val password: String = "",
    @StringRes val passwordError: Int? = null,
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isSignedIn: Boolean = false
)

private const val ERR_SIGN_IN = "PASSWORD_BASED_SIGN_IN"

class AuthLoginViewModel(
    private val passwordBasedSignInUseCase: PasswordBasedSignInUseCase,
    val identifier: String,
    val firstName: String?
) : ViewModel() {
    private val _state = MutableStateFlow(AuthLoginUiState())
    val state: StateFlow<AuthLoginUiState> = _state.asStateFlow()

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, passwordError = null) }
    }

    private fun validate(): Boolean {
        val error = if (_state.value.password.length < 6) R.string.error_password_min_6 else null
        _state.update { it.copy(passwordError = error) }
        return error == null
    }

    fun signIn() {
        if (!validate()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errors = it.errors - ERR_SIGN_IN) }
            val response = passwordBasedSignInUseCase(
                PasswordBasedSignInRequest(
                    identifier = identifier,
                    password = _state.value.password,
                    deviceCategory = DeviceCategory.MOBILE,
                    deviceName = "Android App"
                )
            )
            when (response) {
                is ApiResponse.Success -> _state.update { it.copy(isLoading = false, isSignedIn = true) }
                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_SIGN_IN to response.message))
                }
            }
        }
    }

    fun clearError(tag: String) {
        _state.update { it.copy(errors = it.errors - tag) }
    }
}
