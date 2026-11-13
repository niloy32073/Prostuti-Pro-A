package com.prostuti.student.util

import com.prostuti.student.data.model.enums.GroupRoutineErrorCode
import com.prostuti.student.data.model.response.ApiResponse
import kotlinx.coroutines.delay

/** Waits before each retry: 1 s, 2 s, 4 s (GROUP_ROUTINE_PLAN.md §6.20). */
val AUTHZ_RETRY_DELAYS_MS = listOf(1_000L, 2_000L, 4_000L)

/**
 * Runs [call] again, with backoff, while the server answers `AUTHZ_UNAVAILABLE` (its permission check
 * was down, so nothing ran). Any other answer, `INSUFFICIENT_SCOPE` included, comes back at once.
 */
suspend fun <T> retryWhileAuthzUnavailable(
    delaysMs: List<Long> = AUTHZ_RETRY_DELAYS_MS,
    call: suspend () -> ApiResponse<T>,
): ApiResponse<T> {
    var result = call()
    for (wait in delaysMs) {
        if ((result as? ApiResponse.Failure)?.errorCode != GroupRoutineErrorCode.AUTHZ_UNAVAILABLE) return result
        delay(wait)
        result = call()
    }
    return result
}
