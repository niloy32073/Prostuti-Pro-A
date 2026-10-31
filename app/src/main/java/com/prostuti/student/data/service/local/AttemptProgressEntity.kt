package com.prostuti.student.data.service.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Time a student has spent inside a (practice) exam attempt, saved on this device. Practice time
 * pauses while the app is closed, so on resume the app needs this — the server only knows the
 * time of the last saved answer (`response_time`). Deleted once the attempt is submitted.
 */
@Entity(tableName = "attempt_progress")
data class AttemptProgressEntity(
    @PrimaryKey @ColumnInfo(name = "attempt_id") val attemptId: String,
    @ColumnInfo(name = "elapsed_ms") val elapsedMs: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long
)
