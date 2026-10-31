package com.prostuti.student.data.service.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

/** Stores a `List<String>` column as a JSON array. */
class StringListConverter {
    @TypeConverter
    fun fromList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toList(value: String): List<String> = Json.decodeFromString(value)
}
