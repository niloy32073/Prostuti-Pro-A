package com.prostuti.student.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Reads an enum by its constant name and maps any value the app doesn't know to [fallback]
 * instead of failing the whole response. The server adds enum values over time (for example
 * `exam_type: "SCHEDULED"`), and the API contract says clients must not crash on them.
 */
open class FallbackEnumSerializer<T : Enum<T>>(
    serialName: String,
    private val values: List<T>,
    private val fallback: T,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T {
        val raw = decoder.decodeString()
        return values.firstOrNull { it.name == raw } ?: fallback
    }

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.name)
}
