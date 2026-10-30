package com.prostuti.student.domain

import com.prostuti.student.data.model.request.SendOTPRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.SendOTPResponse
import com.prostuti.student.data.repository.AuthRepository

class SendOTPUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(body: SendOTPRequest): ApiResponse<SendOTPResponse> =
        authRepository.sendOTP(body)
}
