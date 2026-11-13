package com.prostuti.student.ui.screen.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prostuti.student.R
import com.prostuti.student.data.model.request.CheckUserRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.domain.CheckUserUseCase
import com.prostuti.student.util.validateBdPhone
import com.prostuti.student.util.validateEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthStartUiState(
    val identifierTextFieldState: TextFieldState = TextFieldState(""),
    @StringRes val identifierError: Int? = null,
    val isLoading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val accountExists: Boolean? = null,
    val firstName: String? = null
)

private const val ERR_CHECK_USER = "CHECK_USER"

class AuthStartViewModel(private val checkUserUseCase: CheckUserUseCase) : ViewModel() {
    private val _state = MutableStateFlow(AuthStartUiState())
    val state: StateFlow<AuthStartUiState> = _state.asStateFlow()

    init {
        val identifier = _state.value.identifierTextFieldState
        viewModelScope.launch {
            snapshotFlow { identifier.text.toString() }.collect {
                _state.update { it.copy(identifierError = null, accountExists = null) }
            }
        }
    }

    private fun validate(): Boolean {
        val identifier = _state.value.identifierTextFieldState.text.toString().trim()
        val error = when {
            identifier.isEmpty() -> R.string.error_identifier_required
            identifier.any { it.isLetter() } -> if (validateEmail(identifier)) null else R.string.error_invalid_email
            else -> if (validateBdPhone(identifier)) null else R.string.error_invalid_phone
        }
        _state.update { it.copy(identifierError = error) }
        return error == null
    }

    fun continueClicked() {
        if (!validate()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errors = it.errors - ERR_CHECK_USER) }
            val response = checkUserUseCase(CheckUserRequest(identifier = _state.value.identifierTextFieldState.text.toString().trim()))
            when (response) {
                is ApiResponse.Success -> _state.update {
                    it.copy(isLoading = false, accountExists = response.data.exists, firstName = response.data.firstName)
                }

                is ApiResponse.Failure -> _state.update {
                    it.copy(isLoading = false, errors = it.errors + (ERR_CHECK_USER to response.message))
                }
            }
        }
    }

    fun clearError(tag: String) {
        _state.update { it.copy(errors = it.errors - tag) }
    }

    fun consumeNavigation() {
        _state.update { it.copy(accountExists = null) }
    }
}
