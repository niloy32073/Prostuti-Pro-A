package com.prostuti.student.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.prostuti.student.dataStore.PrefKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class StorageRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : StorageRepository {

    override fun getString(key: String): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey(key)] }

    override suspend fun getJWTToken(): String? =
        dataStore.data.map { it[stringPreferencesKey(PrefKeys.JWT_TOKEN)] }.first()

    override suspend fun getRefreshToken(): String? =
        dataStore.data.map { it[stringPreferencesKey(PrefKeys.JWT_REFRESH_TOKEN)] }.first()

    override suspend fun putString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override fun getLong(key: String): Flow<Long?> =
        dataStore.data.map { it[longPreferencesKey(key)] }

    override suspend fun putLong(key: String, value: Long) {
        dataStore.edit { it[longPreferencesKey(key)] = value }
    }

    override fun getBoolean(key: String): Flow<Boolean?> =
        dataStore.data.map { it[booleanPreferencesKey(key)] }

    override suspend fun putBoolean(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
