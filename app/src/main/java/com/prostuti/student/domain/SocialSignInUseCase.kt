package com.prostuti.student.domain

import com.prostuti.student.data.model.enums.SocialProvider
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.LogInResponse
import com.prostuti.student.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SocialSignInUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(provider: SocialProvider, token: String): ApiResponse<LogInResponse> =
        withContext(Dispatchers.IO) { authRepository.socialSignIn(provider, token) }
}
