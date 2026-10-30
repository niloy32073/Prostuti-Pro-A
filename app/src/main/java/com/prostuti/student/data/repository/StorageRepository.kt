package com.prostuti.student.data.repository

import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun getString(key: String): Flow<String?>

    suspend fun getJWTToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun putString(key: String, value: String)
    fun getLong(key: String): Flow<Long?>
    suspend fun putLong(key: String, value: Long)
    fun getBoolean(key: String): Flow<Boolean?>
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun remove(key: String)
    suspend fun clearAll()
}
