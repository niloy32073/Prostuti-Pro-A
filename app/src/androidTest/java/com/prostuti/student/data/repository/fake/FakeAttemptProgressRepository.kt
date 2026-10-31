package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.repository.AttemptProgressRepository

/** In-memory [AttemptProgressRepository] — the locally saved time per attempt. */
class FakeAttemptProgressRepository(initial: Map<String, Long> = emptyMap()) : AttemptProgressRepository {
    val saved = initial.toMutableMap()
    val cleared = mutableListOf<String>()

    override suspend fun getElapsedMs(attemptId: String): Long? = saved[attemptId]

    override suspend fun saveElapsedMs(attemptId: String, elapsedMs: Long) {
        saved[attemptId] = elapsedMs
    }

    override suspend fun clear(attemptId: String) {
        saved -= attemptId
        cleared += attemptId
    }
}
