package com.prostuti.student.data.model.enums

import com.prostuti.student.util.FallbackEnumSerializer
import kotlinx.serialization.Serializable


/** What a scheduled exam means for the signed-in student right now. The UI is driven by this,
 * not by recomputing it from the window. Unknown values read as [UPCOMING] (nothing actionable). */
@Serializable(with = SlotMyStatusSerializer::class)
enum class SlotMyStatus {
    UPCOMING, OPEN, IN_PROGRESS, COMPLETED, MISSED
}

object SlotMyStatusSerializer : FallbackEnumSerializer<SlotMyStatus>("SlotMyStatus", SlotMyStatus.entries, SlotMyStatus.UPCOMING)
