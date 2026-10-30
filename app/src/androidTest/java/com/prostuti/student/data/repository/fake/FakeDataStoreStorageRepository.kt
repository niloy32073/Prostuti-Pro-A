package com.prostuti.student.data.repository.fake

import com.prostuti.student.data.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [StorageRepository] fake for OnboardingScreen/SettingsScreen Compose UI tests.
 * Duplicated from the `test/` source set's equivalent — androidTest and test are separate
 * source sets and cannot share classes. */
class FakeDataStoreStorageRepository : StorageRepository {
    private val strings = mutableMapOf<String, MutableStateFlow<String?>>()
    private val longs = mutableMapOf<String, MutableStateFlow<Long?>>()
    private val booleans = mutableMapOf<String, MutableStateFlow<Boolean?>>()

    val putBooleanCalls = mutableListOf<Pair<String, Boolean>>()
    var clearAllCallCount = 0

    override fun getString(key: String): Flow<String?> = strings.getOrPut(key) { MutableStateFlow(null) }
    override suspend fun getJWTToken(): String? = strings["jwt_token"]?.value
    override suspend fun getRefreshToken(): String? = strings["jwt_refresh_token"]?.value
    override suspend fun putString(key: String, value: String) {
        strings.getOrPut(key) { MutableStateFlow(null) }.value = value
    }

    override fun getLong(key: String): Flow<Long?> = longs.getOrPut(key) { MutableStateFlow(null) }
    override suspend fun putLong(key: String, value: Long) {
        longs.getOrPut(key) { MutableStateFlow(null) }.value = value
    }

    override fun getBoolean(key: String): Flow<Boolean?> = booleans.getOrPut(key) { MutableStateFlow(null) }
    override suspend fun putBoolean(key: String, value: Boolean) {
        putBooleanCalls += key to value
        booleans.getOrPut(key) { MutableStateFlow(null) }.value = value
    }

    override suspend fun remove(key: String) {
        strings.remove(key); longs.remove(key); booleans.remove(key)
    }

    override suspend fun clearAll() {
        clearAllCallCount++
        strings.clear(); longs.clear(); booleans.clear()
    }
}
