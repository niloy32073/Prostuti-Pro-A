package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.SocialProvider
import com.prostuti.student.data.model.request.CheckUserRequest
import com.prostuti.student.data.model.request.PasswordBasedSignInRequest
import com.prostuti.student.data.model.request.PasswordBasedSignUpRequest
import com.prostuti.student.data.model.request.ResetPasswordRequest
import com.prostuti.student.data.model.request.SendOTPRequest
import com.prostuti.student.data.model.request.SocialSignInRequest
import com.prostuti.student.data.model.request.VerifyIdentifierRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CheckUserResponse
import com.prostuti.student.data.model.response.LogInResponse
import com.prostuti.student.data.model.response.PasswordBasedSignUpResponse
import com.prostuti.student.data.model.response.ResetTokenResponse
import com.prostuti.student.data.model.response.SendOTPResponse
import com.prostuti.student.data.model.response.VerifyEmailResponse
import com.prostuti.student.data.model.response.VerifyPhoneResponse
import com.prostuti.student.data.service.remote.AuthApiService
import com.prostuti.student.dataStore.PrefKeys

class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val storageRepository: StorageRepository
) : AuthRepository {

    override suspend fun checkUser(body: CheckUserRequest): ApiResponse<CheckUserResponse> =
        authApiService.checkUser(body)

    override suspend fun passwordBasedSignUp(body: PasswordBasedSignUpRequest): ApiResponse<PasswordBasedSignUpResponse> =
        authApiService.passwordBasedSignUp(body)

    override suspend fun passwordBasedSignIn(body: PasswordBasedSignInRequest): ApiResponse<LogInResponse> {
        val response = authApiService.passwordBasedSignIn(body)
        persistTokensOnSuccess(response)
        return response
    }

    override suspend fun socialSignIn(provider: SocialProvider, token: String): ApiResponse<LogInResponse> {
        val response = authApiService.socialSignIn(SocialSignInRequest(provider = provider.name, token = token))
        persistTokensOnSuccess(response)
        return response
    }

    override suspend fun sendOTP(body: SendOTPRequest): ApiResponse<SendOTPResponse> =
        authApiService.sendOTP(body)

    override suspend fun verifyEmail(body: VerifyIdentifierRequest): ApiResponse<VerifyEmailResponse> =
        authApiService.verifyEmail(body)

    override suspend fun verifyPhone(body: VerifyIdentifierRequest): ApiResponse<VerifyPhoneResponse> =
        authApiService.verifyPhone(body)

    override suspend fun verifyResetOtp(body: VerifyIdentifierRequest): ApiResponse<ResetTokenResponse> =
        authApiService.verifyResetOtp(body)

    override suspend fun resetPassword(body: ResetPasswordRequest): ApiResponse<LogInResponse> {
        val response = authApiService.resetPassword(body)
        persistTokensOnSuccess(response)
        return response
    }

    private suspend fun persistTokensOnSuccess(response: ApiResponse<LogInResponse>) {
        if (response is ApiResponse.Success) {
            storageRepository.putString(key = PrefKeys.JWT_TOKEN, value = response.data.accessToken)
            storageRepository.putString(key = PrefKeys.JWT_REFRESH_TOKEN, value = response.data.refreshToken)
        }
    }
}
