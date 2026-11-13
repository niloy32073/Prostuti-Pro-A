package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.model.enums.SocialProvider
import com.prostuti.student.data.model.enums.UserRole
import com.prostuti.student.data.model.enums.UserStatus
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
import com.prostuti.student.data.model.response.SendOTPResponse
import com.prostuti.student.data.model.response.VerifyEmailResponse
import com.prostuti.student.data.model.response.VerifyPhoneResponse
import com.prostuti.student.data.repository.AuthRepository

fun fakeLogInResponse() = LogInResponse(
    accessToken = "fake_access_token",
    email = "test@example.com",
    expiresIn = 3600,
    firstName = "Test",
    isEmailVerified = true,
    isPhoneVerified = true,
    lastName = "User",
    refreshToken = "fake_refresh_token",
    role = UserRole.STUDENT,
    status = "ACTIVE",
    tokenType = "Bearer",
    userId = "u1",
    username = "testuser"
)

/** A successful sign-up response; [username] is the only field the tests check. */
fun fakeSignUpResponse(username: String = "testuser") = PasswordBasedSignUpResponse(
    accessToken = "fake_access_token",
    email = "test@example.com",
    expiresIn = 3600,
    firstName = "Test",
    lastName = "User",
    refreshToken = "fake_refresh_token",
    status = UserStatus.PENDING,
    tokenType = "Bearer",
    userId = "018f3c7e-007b-7000-8000-000000000001",
    username = username
)

/** Configurable fake of [AuthRepository]; every method's result is a settable var defaulting to Success. */
class FakeAuthRepository(
    var checkUserResult: ApiResponse<CheckUserResponse> = ApiResponse.Success(CheckUserResponse(exists = false, firstName = null)),
    var signUpResult: ApiResponse<PasswordBasedSignUpResponse> = ApiResponse.Success(
        fakeSignUpResponse()
    ),
    var signInResult: ApiResponse<LogInResponse> = ApiResponse.Success(fakeLogInResponse()),
    var socialSignInResult: ApiResponse<LogInResponse> = ApiResponse.Success(fakeLogInResponse()),
    var sendOtpResult: ApiResponse<SendOTPResponse> = ApiResponse.Success(SendOTPResponse(otp = "123456")),
    var verifyEmailResult: ApiResponse<VerifyEmailResponse> = ApiResponse.Success(VerifyEmailResponse(UserStatus.ACTIVE, isEmailVerified = true)),
    var verifyPhoneResult: ApiResponse<VerifyPhoneResponse> = ApiResponse.Success(VerifyPhoneResponse(UserStatus.ACTIVE, isPhoneVerified = true)),
    var resetPasswordResult: ApiResponse<Unit> = ApiResponse.Success(Unit)
) : AuthRepository {

    var lastCheckUserRequest: CheckUserRequest? = null
    var lastSignUpRequest: PasswordBasedSignUpRequest? = null
    var lastSignInRequest: PasswordBasedSignInRequest? = null
    var lastSendOtpRequest: SendOTPRequest? = null
    var lastVerifyEmailRequest: VerifyIdentifierRequest? = null
    var lastVerifyPhoneRequest: VerifyIdentifierRequest? = null
    var lastResetPasswordRequest: ResetPasswordRequest? = null
    var sendOtpCallCount = 0

    override suspend fun checkUser(body: CheckUserRequest): ApiResponse<CheckUserResponse> {
        lastCheckUserRequest = body
        return checkUserResult
    }

    override suspend fun passwordBasedSignUp(body: PasswordBasedSignUpRequest): ApiResponse<PasswordBasedSignUpResponse> {
        lastSignUpRequest = body
        return signUpResult
    }

    override suspend fun passwordBasedSignIn(body: PasswordBasedSignInRequest): ApiResponse<LogInResponse> {
        lastSignInRequest = body
        return signInResult
    }

    override suspend fun socialSignIn(provider: SocialProvider, token: String): ApiResponse<LogInResponse> = socialSignInResult

    override suspend fun sendOTP(body: SendOTPRequest): ApiResponse<SendOTPResponse> {
        lastSendOtpRequest = body
        sendOtpCallCount++
        return sendOtpResult
    }

    override suspend fun verifyEmail(body: VerifyIdentifierRequest): ApiResponse<VerifyEmailResponse> {
        lastVerifyEmailRequest = body
        return verifyEmailResult
    }

    override suspend fun verifyPhone(body: VerifyIdentifierRequest): ApiResponse<VerifyPhoneResponse> {
        lastVerifyPhoneRequest = body
        return verifyPhoneResult
    }

    override suspend fun resetPassword(body: ResetPasswordRequest): ApiResponse<Unit> {
        lastResetPasswordRequest = body
        return resetPasswordResult
    }
}
