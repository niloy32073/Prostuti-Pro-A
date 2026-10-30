package com.prostuti.student.domain

import com.prostuti.student.data.model.request.CheckUserRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CheckUserResponse
import com.prostuti.student.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(body: CheckUserRequest): ApiResponse<CheckUserResponse> =
        withContext(Dispatchers.IO) { authRepository.checkUser(body) }
}
