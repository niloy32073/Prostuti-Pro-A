package com.prostuti.student.util

import io.ktor.client.statement.HttpResponse
import timber.log.Timber
import java.io.IOException

/**
 * Logs a failed API call. Bug-type failures go out as `Timber.e` (a Crashlytics non-fatal in release);
 * network/timeout/connection failures (any [IOException]) are only `Timber.w` (a breadcrumb) — they are
 * the user's connection, not a bug.
 */
fun logApiFailure(caller: String, errorCode: String, e: Exception) {
    val tree = Timber.tag(ApiLog.TAG)
    if (errorCode == NETWORK_ERROR || errorCode == TIMEOUT_ERROR || e is IOException) {
        tree.w("%s failed: %s (%s)", caller, errorCode, e.javaClass.simpleName)
    } else {
        tree.e(e, "%s failed: %s", caller, errorCode)
    }
}

/** A 5xx answer is a server bug: report it as a non-fatal. 4xx answers are normal app flow and are not reported. */
fun logServerError(response: HttpResponse, errorCode: String) {
    if (response.status.value < 500) return
    // Path only — the query string can hold user input.
    val path = response.call.request.url.encodedPath
    Timber.tag(ApiLog.TAG).e(ServerErrorException(response.status.value, path, errorCode))
}

class ServerErrorException(status: Int, path: String, errorCode: String) :
    Exception("HTTP $status at $path (code $errorCode)")

const val NETWORK_ERROR = "NETWORK_ERROR"
const val TIMEOUT_ERROR = "TIMEOUT_ERROR"
