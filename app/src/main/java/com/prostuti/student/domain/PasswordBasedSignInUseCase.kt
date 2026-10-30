package com.prostuti.student.domain

import com.prostuti.student.data.model.request.PasswordBasedSignInRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.LogInResponse
import com.prostuti.student.data.repository.AuthRepository

class PasswordBasedSignInUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(body: PasswordBasedSignInRequest): ApiResponse<LogInResponse> =
        authRepository.passwordBasedSignIn(body)
}
