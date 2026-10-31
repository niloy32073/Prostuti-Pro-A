package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.service.local.CurrentUserDao
import com.prostuti.student.data.service.local.CurrentUserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory single-row stand-in for the Room [CurrentUserDao]. */
class FakeCurrentUserDao(initial: CurrentUserEntity? = null) : CurrentUserDao {
    private val row = MutableStateFlow(initial)
    var replaceCount = 0

    override suspend fun insert(user: CurrentUserEntity) { row.value = user }
    override suspend fun get(): CurrentUserEntity? = row.value
    override fun observe(): Flow<CurrentUserEntity?> = row
    override suspend fun deleteAll() { row.value = null }
    override suspend fun replace(user: CurrentUserEntity) { replaceCount++; row.value = user }
}
