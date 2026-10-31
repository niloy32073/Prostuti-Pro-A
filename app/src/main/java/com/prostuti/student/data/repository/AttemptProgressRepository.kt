package com.prostuti.student.data.repository

/** Locally saved time spent in an exam attempt (see AttemptProgressEntity). */
interface AttemptProgressRepository {
    /** Saved elapsed time in milliseconds, or null if this device has none (e.g. after a reinstall). */
    suspend fun getElapsedMs(attemptId: String): Long?
    suspend fun saveElapsedMs(attemptId: String, elapsedMs: Long)
    suspend fun clear(attemptId: String)
}
