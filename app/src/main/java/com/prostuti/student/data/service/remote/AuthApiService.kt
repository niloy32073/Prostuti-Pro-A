package com.prostuti.student.data.service.remote

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
import com.prostuti.student.data.model.response.MeData
import com.prostuti.student.data.model.response.PasswordBasedSignUpResponse
import com.prostuti.student.data.model.response.ResetTokenResponse
import com.prostuti.student.data.model.response.SendOTPResponse
import com.prostuti.student.data.model.response.VerifyEmailResponse
import com.prostuti.student.data.model.response.VerifyPhoneResponse
import com.prostuti.student.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApiService(private val client: HttpClient) {
    suspend fun checkUser(body: CheckUserRequest): ApiResponse<CheckUserResponse> = safeApiCall {
        client.post("auth/check-user") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun passwordBasedSignUp(body: PasswordBasedSignUpRequest): ApiResponse<PasswordBasedSignUpResponse> = safeApiCall {
        client.post("auth/register") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun passwordBasedSignIn(body: PasswordBasedSignInRequest): ApiResponse<LogInResponse> = safeApiCall {
        client.post("auth/login") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun socialSignIn(body: SocialSignInRequest): ApiResponse<LogInResponse> = safeApiCall {
        client.post("auth/social-login") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun sendOTP(body: SendOTPRequest): ApiResponse<SendOTPResponse> = safeApiCall {
        client.post("auth/otp/send") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun verifyEmail(body: VerifyIdentifierRequest): ApiResponse<VerifyEmailResponse> = safeApiCall {
        client.post("auth/otp/verify-email") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun verifyPhone(body: VerifyIdentifierRequest): ApiResponse<VerifyPhoneResponse> = safeApiCall {
        client.post("auth/otp/verify-phone") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    /** Verifies a PASSWORD_RESET-purpose OTP and exchanges it for a short-lived reset token. */
    suspend fun verifyResetOtp(body: VerifyIdentifierRequest): ApiResponse<ResetTokenResponse> = safeApiCall {
        client.post("auth/otp/verify-reset") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun resetPassword(body: ResetPasswordRequest): ApiResponse<LogInResponse> = safeApiCall {
        client.post("auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    /**
     * The signed-in user's profile. [includeDenied] adds `denied_scopes`/`denied_roles`; the
     * query parameter is left off entirely when false, so the default response stays small.
     */
    suspend fun getMe(includeDenied: Boolean = false): ApiResponse<MeData> = safeApiCall {
        client.get("auth/me") {
            if (includeDenied) parameter("include_denied", true)
        }
    }
}
