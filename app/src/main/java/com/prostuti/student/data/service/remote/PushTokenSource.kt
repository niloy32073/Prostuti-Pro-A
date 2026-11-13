package com.prostuti.student.data.service.remote

/** Gives this device's push token. Separate from Firebase so repositories can be tested with a fake. */
interface PushTokenSource {
    /** The current token, or null when it can't be fetched (no Play services, no network, …). */
    suspend fun currentToken(): String?
}
