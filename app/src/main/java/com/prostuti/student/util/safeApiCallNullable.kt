package com.prostuti.student.util

import com.prostuti.student.data.model.response.ApiResponse
import io.ktor.client.statement.HttpResponse

/**
 * Like [safeApiCall], for endpoints where `"data": null` on success is a real answer ("none"),
 * e.g. `GET /routines/active` with no active routine. [safeApiCall] reports a 2xx response with null
 * data as a Failure whose code is the HTTP status (an error body always carries its own `error.code`);
 * this turns exactly that case into `Success(null)`.
 */
suspend inline fun <reified T> safeApiCallNullable(
    crossinline execute: suspend () -> HttpResponse
): ApiResponse<T?> = when (val result = safeApiCall<T> { execute() }) {
    is ApiResponse.Success -> result
    is ApiResponse.Failure ->
        if (result.errorCode.length == 3 && result.errorCode.startsWith("2")) ApiResponse.Success(null, result.message)
        else result
}
