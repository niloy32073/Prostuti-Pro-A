package com.prostuti.student.data.repository

import com.prostuti.student.data.service.local.AttemptProgressDao
import com.prostuti.student.data.service.local.AttemptProgressEntity
import com.prostuti.student.util.TimeProvider

class AttemptProgressRepositoryImpl(
    private val dao: AttemptProgressDao,
    private val timeProvider: TimeProvider
) : AttemptProgressRepository {

    override suspend fun getElapsedMs(attemptId: String): Long? = dao.getByAttemptId(attemptId)?.elapsedMs

    override suspend fun saveElapsedMs(attemptId: String, elapsedMs: Long) {
        dao.upsert(AttemptProgressEntity(attemptId, elapsedMs, timeProvider.nowEpochMillis()))
    }

    override suspend fun clear(attemptId: String) = dao.deleteByAttemptId(attemptId)
}
