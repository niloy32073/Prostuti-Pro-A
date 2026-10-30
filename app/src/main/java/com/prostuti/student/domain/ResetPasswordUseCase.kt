package com.prostuti.student.domain

import com.prostuti.student.data.model.request.ResetPasswordRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.LogInResponse
import com.prostuti.student.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(body: ResetPasswordRequest): ApiResponse<LogInResponse> =
        withContext(Dispatchers.IO) { authRepository.resetPassword(body) }
}
