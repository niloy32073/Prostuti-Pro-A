package com.prostuti.student.domain

import com.prostuti.student.data.model.request.PasswordBasedSignInRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.LogInResponse
import com.prostuti.student.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PasswordBasedSignInUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(body: PasswordBasedSignInRequest): ApiResponse<LogInResponse> =
        withContext(Dispatchers.IO) { authRepository.passwordBasedSignIn(body) }
}
