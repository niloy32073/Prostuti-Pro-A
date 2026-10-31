package com.prostuti.student.domain

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MeData
import com.prostuti.student.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow

/** Observes the signed-in user's profile; [refresh] forces a fresh `GET auth/me`. */
class GetMeUseCase(private val repository: UserRepository) {
    operator fun invoke(refresh: Boolean = false): Flow<ApiResponse<MeData>> = repository.observeMe(refresh)
}
