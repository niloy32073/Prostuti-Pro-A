package com.prostuti.student.data.repository

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.MeData
import com.prostuti.student.data.service.local.CurrentUserDao
import com.prostuti.student.data.service.local.toEntity
import com.prostuti.student.data.service.local.toMeData
import com.prostuti.student.data.service.remote.AuthApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val authApiService: AuthApiService,
    private val currentUserDao: CurrentUserDao
) : UserRepository {

    override fun observeMe(refresh: Boolean): Flow<ApiResponse<MeData>> = flow {
        if (refresh || currentUserDao.get() == null) {
            when (val api = authApiService.getMe()) {
                is ApiResponse.Success -> currentUserDao.replace(api.data.toEntity())
                is ApiResponse.Failure -> {
                    emit(api)
                    if (currentUserDao.get() == null) return@flow
                }
            }
        }
        emitAll(currentUserDao.observe().filterNotNull().map { ApiResponse.Success(it.toMeData()) })
    }

    override suspend fun clearCachedMe() = currentUserDao.deleteAll()
}
