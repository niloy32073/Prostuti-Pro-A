package com.prostuti.student.data.repository

import com.prostuti.student.data.model.widget.WidgetSnapshot
import com.prostuti.student.dataStore.PrefKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber

/** Stores the snapshot as JSON in the app's DataStore (cleared with everything else on logout). */
class WidgetSnapshotRepositoryImpl(
    private val storage: StorageRepository,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : WidgetSnapshotRepository {

    override fun observe(): Flow<WidgetSnapshot?> = storage.getString(PrefKeys.WIDGET_SNAPSHOT).map { decode(it) }

    override suspend fun current(): WidgetSnapshot? = observe().first()

    override suspend fun save(snapshot: WidgetSnapshot) =
        storage.putString(PrefKeys.WIDGET_SNAPSHOT, json.encodeToString(WidgetSnapshot.serializer(), snapshot))

    private fun decode(text: String?): WidgetSnapshot? {
        if (text.isNullOrBlank()) return null
        return try {
            json.decodeFromString(WidgetSnapshot.serializer(), text)
        } catch (e: SerializationException) {
            // An older app version's shape: drop it, the next refresh writes a new one.
            Timber.w(e, "Widget snapshot unreadable")
            null
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Widget snapshot unreadable")
            null
        }
    }
}
