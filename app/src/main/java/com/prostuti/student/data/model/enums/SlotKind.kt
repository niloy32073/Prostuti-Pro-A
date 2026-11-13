package com.prostuti.student.data.model.enums

import com.prostuti.student.util.FallbackEnumSerializer
import kotlinx.serialization.Serializable


/** Where a scheduled exam came from: a routine's day, or a one-off exam an admin added. */
@Serializable(with = SlotKindSerializer::class)
enum class SlotKind {
    ROUTINE, ADHOC
}

object SlotKindSerializer : FallbackEnumSerializer<SlotKind>("SlotKind", SlotKind.entries, SlotKind.ROUTINE)
