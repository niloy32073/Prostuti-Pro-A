package com.prostuti.student.domain

import com.prostuti.student.data.model.request.VerifyIdentifierRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.VerifyPhoneResponse
import com.prostuti.student.data.repository.AuthRepository

class VerifyPhoneUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(body: VerifyIdentifierRequest): ApiResponse<VerifyPhoneResponse> =
        authRepository.verifyPhone(body)
}
