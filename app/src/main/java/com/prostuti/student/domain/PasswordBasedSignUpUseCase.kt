package com.prostuti.student.domain

import com.prostuti.student.data.model.request.PasswordBasedSignUpRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.PasswordBasedSignUpResponse
import com.prostuti.student.data.repository.AuthRepository

class PasswordBasedSignUpUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(body: PasswordBasedSignUpRequest): ApiResponse<PasswordBasedSignUpResponse> =
        authRepository.passwordBasedSignUp(body)
}
