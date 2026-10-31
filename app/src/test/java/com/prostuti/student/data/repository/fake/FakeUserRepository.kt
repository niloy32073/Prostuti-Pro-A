package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MeData
import com.prostuti.student.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Emits [results] in order for every `observeMe` call and records the refresh flags. */
class FakeUserRepository(
    var results: List<ApiResponse<MeData>> = emptyList()
) : UserRepository {
    val refreshArgs = mutableListOf<Boolean>()
    var clearCount = 0

    override fun observeMe(refresh: Boolean): Flow<ApiResponse<MeData>> {
        refreshArgs += refresh
        return flowOf(*results.toTypedArray())
    }

    override suspend fun clearCachedMe() { clearCount++ }

    companion object {
        fun me(
            firstName: String = "Muwaz",
            lastName: String = "Ahmed",
            username: String = "muwy",
            email: String? = "muwy@proton.me",
            phone: String? = "+8801712345678",
            profileImage: String? = null,
            isEmailVerified: Boolean = true,
            isPhoneVerified: Boolean = false,
            lastLoginAt: String? = "2026-06-20T10:30:00Z"
        ) = MeData(
            userId = "018f3c7e-007b-7000-8000-000000000001",
            firstName = firstName, lastName = lastName, username = username,
            email = email, phone = phone, profileImage = profileImage, gender = "MALE",
            role = "STUDENT", status = "ACTIVE", provider = "EMAIL",
            isEmailVerified = isEmailVerified, isPhoneVerified = isPhoneVerified,
            lastLoginAt = lastLoginAt, createdAt = "2025-01-15T08:00:00Z",
            scopes = listOf("curriculum:read", "exam:attempt", "exam:read")
        )
    }
}
