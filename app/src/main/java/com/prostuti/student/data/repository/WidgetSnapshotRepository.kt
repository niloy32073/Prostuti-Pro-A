package com.prostuti.student.data.repository

import com.prostuti.student.data.model.widget.WidgetSnapshot
import kotlinx.coroutines.flow.Flow

/** The home screen widget's last loaded data. */
interface WidgetSnapshotRepository {
    /** Null = nothing stored yet. */
    fun observe(): Flow<WidgetSnapshot?>
    suspend fun current(): WidgetSnapshot?
    suspend fun save(snapshot: WidgetSnapshot)
}
