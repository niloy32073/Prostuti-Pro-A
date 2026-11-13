package com.prostuti.student.domain

import com.prostuti.student.data.model.request.ResetPasswordRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.AuthRepository

class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(body: ResetPasswordRequest): ApiResponse<Unit> =
        authRepository.resetPassword(body)
}
