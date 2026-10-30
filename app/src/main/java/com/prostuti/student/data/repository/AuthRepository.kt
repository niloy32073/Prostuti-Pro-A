package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.SocialProvider
import com.prostuti.student.data.model.request.CheckUserRequest
import com.prostuti.student.data.model.request.PasswordBasedSignInRequest
import com.prostuti.student.data.model.request.PasswordBasedSignUpRequest
import com.prostuti.student.data.model.request.ResetPasswordRequest
import com.prostuti.student.data.model.request.SendOTPRequest
import com.prostuti.student.data.model.request.VerifyIdentifierRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.CheckUserResponse
import com.prostuti.student.data.model.response.LogInResponse
import com.prostuti.student.data.model.response.PasswordBasedSignUpResponse
import com.prostuti.student.data.model.response.ResetTokenResponse
import com.prostuti.student.data.model.response.SendOTPResponse
import com.prostuti.student.data.model.response.VerifyEmailResponse
import com.prostuti.student.data.model.response.VerifyPhoneResponse

interface AuthRepository {
    suspend fun checkUser(body: CheckUserRequest): ApiResponse<CheckUserResponse>
    suspend fun passwordBasedSignUp(body: PasswordBasedSignUpRequest): ApiResponse<PasswordBasedSignUpResponse>
    suspend fun passwordBasedSignIn(body: PasswordBasedSignInRequest): ApiResponse<LogInResponse>
    suspend fun socialSignIn(provider: SocialProvider, token: String): ApiResponse<LogInResponse>
    suspend fun sendOTP(body: SendOTPRequest): ApiResponse<SendOTPResponse>
    suspend fun verifyEmail(body: VerifyIdentifierRequest): ApiResponse<VerifyEmailResponse>
    suspend fun verifyPhone(body: VerifyIdentifierRequest): ApiResponse<VerifyPhoneResponse>
    suspend fun verifyResetOtp(body: VerifyIdentifierRequest): ApiResponse<ResetTokenResponse>
    suspend fun resetPassword(body: ResetPasswordRequest): ApiResponse<LogInResponse>
}
