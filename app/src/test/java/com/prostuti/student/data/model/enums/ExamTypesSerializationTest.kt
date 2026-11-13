package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamTypesSerializationTest {
    @Serializable private data class Holder(val type: ExamTypes)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test fun `known values decode by name`() {
        assertEquals(ExamTypes.BOARD, json.decodeFromString<Holder>("""{"type":"BOARD"}""").type)
        assertEquals(ExamTypes.SCHEDULED, json.decodeFromString<Holder>("""{"type":"SCHEDULED"}""").type)
    }

    @Test fun `an unknown value decodes to UNKNOWN instead of failing`() {
        assertEquals(ExamTypes.UNKNOWN, json.decodeFromString<Holder>("""{"type":"SOMETHING_NEW"}""").type)
    }

    @Test fun `encodes by name`() {
        assertEquals("""{"type":"SCHEDULED"}""", json.encodeToString(Holder(ExamTypes.SCHEDULED)))
    }
}
