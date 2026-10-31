package com.prostuti.student.util

import com.prostuti.student.BuildConfig
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.BaseResponse
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import timber.log.Timber
import java.net.UnknownHostException

suspend inline fun <reified T> safeApiCall(
    crossinline execute: suspend () -> HttpResponse
): ApiResponse<T> {
    return try {
        val response = execute()
        // Raw JSON first, so a body that fails to decode below is still visible in the log.
        // Debug only: bodies carry personal data (tokens are masked).
        if (BuildConfig.DEBUG) ApiLog.response(response)
        if (response.status.isSuccess()) {
            val baseResponse = response.body<BaseResponse<T>>()
            if (BuildConfig.DEBUG) ApiLog.decoded(response.call.request.url.toString(), baseResponse)

            if (baseResponse.success && baseResponse.data != null) {
                ApiResponse.Success(
                    data = baseResponse.data,
                    message = baseResponse.message.orEmpty(),
                    metaData = baseResponse.metaData
                )
            } else {
                // Backend sent HTTP 2xx, but the operation failed (e.g. success: false)
                ApiResponse.Failure(
                    message = baseResponse.message ?: "Operation failed",
                    errorCode = baseResponse.error?.code ?: response.status.value.toString(),
                    details = baseResponse.error?.details
                )
            }
        } else {
            val errorBody = try {
                response.body<BaseResponse<Unit>>()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }

            ApiResponse.Failure(
                message = errorBody?.message
                    ?: errorBody?.error?.details
                    ?: "Unexpected server response (${response.status.value})",
                errorCode = errorBody?.error?.code ?: response.status.value.toString(),
                details = errorBody?.error?.details
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val (errorMessage, errorCode) = when (e) {
            is SocketTimeoutException, is HttpRequestTimeoutException -> "Timeout: Server is too slow" to "TIMEOUT_ERROR"
            is UnknownHostException, is UnresolvedAddressException -> "No internet connection" to "NETWORK_ERROR"
            is SerializationException -> "Mapping Error: Check your data classes" to "SERIALIZATION_ERROR"
            else -> (e.message ?: "Unknown error") to "UNKNOWN_ERROR"
        }
        Timber.tag(ApiLog.TAG).e(e, "safeApiCall failed: %s", errorCode)

        ApiResponse.Failure(
            message = errorMessage,
            errorCode = errorCode,
            details = e.localizedMessage
        )
    }
}
