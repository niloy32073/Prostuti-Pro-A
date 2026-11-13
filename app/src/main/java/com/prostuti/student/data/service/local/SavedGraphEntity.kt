package com.prostuti.student.data.service.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A graph the student saved from the graph builder. [content] is the builder's own JSON (lines,
 * settings, view); [preview] is a short text of its first lines for the "My graphs" list.
 */
@Entity(tableName = "saved_graph")
data class SavedGraphEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "preview") val preview: String,
    @ColumnInfo(name = "is_space") val isSpace: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
