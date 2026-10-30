package com.prostuti.student.domain

import com.prostuti.student.data.model.request.VerifyIdentifierRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.VerifyEmailResponse
import com.prostuti.student.data.repository.AuthRepository

class VerifyEmailUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(body: VerifyIdentifierRequest): ApiResponse<VerifyEmailResponse> =
        authRepository.verifyEmail(body)
}
