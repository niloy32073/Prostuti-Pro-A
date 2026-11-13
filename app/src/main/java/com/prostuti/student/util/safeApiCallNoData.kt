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
import kotlinx.serialization.json.JsonElement
import java.net.UnknownHostException

/**
 * Like [safeApiCall], for endpoints whose success body has `"data": null` (e.g. a DELETE).
 * [safeApiCall] would report those as failures, since it needs `data`.
 */
suspend inline fun safeApiCallNoData(
    crossinline execute: suspend () -> HttpResponse
): ApiResponse<Unit> {
    return try {
        val response = execute()
        if (BuildConfig.DEBUG) ApiLog.response(response)
        val baseResponse = try {
            response.body<BaseResponse<JsonElement>>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (response.status.isSuccess() && baseResponse?.success != false) {
            ApiResponse.Success(data = Unit, message = baseResponse?.message.orEmpty())
        } else {
            val errorCode = baseResponse?.error?.code ?: response.status.value.toString()
            logServerError(response, errorCode)
            ApiResponse.Failure(
                message = baseResponse?.message
                    ?: baseResponse?.error?.details
                    ?: "Unexpected server response (${response.status.value})",
                errorCode = errorCode,
                details = baseResponse?.error?.details
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val (errorMessage, errorCode) = when (e) {
            is SocketTimeoutException, is HttpRequestTimeoutException -> "Timeout: Server is too slow" to TIMEOUT_ERROR
            is UnknownHostException, is UnresolvedAddressException -> "No internet connection" to NETWORK_ERROR
            is SerializationException -> "Mapping Error: Check your data classes" to "SERIALIZATION_ERROR"
            else -> (e.message ?: "Unknown error") to "UNKNOWN_ERROR"
        }
        logApiFailure("safeApiCallNoData", errorCode, e)
        ApiResponse.Failure(message = errorMessage, errorCode = errorCode, details = e.localizedMessage)
    }
}
