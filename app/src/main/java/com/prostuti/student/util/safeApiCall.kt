package com.prostuti.student.util

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.BaseResponse
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import timber.log.Timber

suspend inline fun <reified T> safeApiCall(
    crossinline execute: suspend () -> HttpResponse
): ApiResponse<T> {
    return try {
        val response = execute()
        val url = response.call.request.url
        Timber.d("API Success | URL: $url | Json: ${response.bodyAsText()}")
        if (response.status.isSuccess()) {
            val baseResponse = response.body<BaseResponse<T>>()

            if (baseResponse.success && baseResponse.data != null) {
                Timber.d("API Success | URL: $url | Data: ${baseResponse.data}")
                ApiResponse.Success(
                    data = baseResponse.data,
                    message = baseResponse.message.orEmpty(),
                    metaData = baseResponse.metaData
                )
            } else {
                // Backend sent HTTP 200/2xx, but operation failed (e.g., success: false)
                val errorMessage = baseResponse.message ?: "Operation failed"
                val errorCode = baseResponse.error?.code ?: response.status.value.toString()
                val errorDetails = baseResponse.error?.details

                Timber.d("API Operational Failure | URL: $url | Code: $errorCode | Message: $errorMessage")
                ApiResponse.Failure(
                    message = errorMessage,
                    errorCode = errorCode,
                    details = errorDetails
                )
            }
        } else {
            // HTTP status code is non-2xx
            Timber.d("API HTTP Error | URL: $url | Status: ${response.status}")

            val errorBody = try {
                response.body<BaseResponse<Unit>>()
            } catch (e: Exception) {
                null
            }

            val errorMessage = errorBody?.message
                ?: errorBody?.error?.details
                ?: "Unexpected server response (${response.status.value})"

            val errorCode = errorBody?.error?.code ?: response.status.value.toString()

            ApiResponse.Failure(
                message = errorMessage,
                errorCode = errorCode,
                details = errorBody?.error?.details
            )
        }
    } catch (e: Exception) {
        e.message?.let { Timber.e(e, it) }

        val (errorMessage, errorCode) = when (e) {
            is SocketTimeoutException -> "Timeout: Server is too slow" to "TIMEOUT_ERROR"
            is SerializationException -> "Mapping Error: Check your data classes" to "SERIALIZATION_ERROR"
            else -> (e.message ?: "Unknown error") to "UNKNOWN_ERROR"
        }

        ApiResponse.Failure(
            message = errorMessage,
            errorCode = errorCode,
            details = e.localizedMessage
        )
    }
}
