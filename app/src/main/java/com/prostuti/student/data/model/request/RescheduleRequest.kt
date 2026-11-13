package com.prostuti.student.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Body of `PATCH /routines/slots/{id}/reschedule` — the new day (`yyyy-MM-dd`); window times stay the same. */
@Serializable
data class RescheduleRequest(@SerialName("slot_date") val slotDate: String)
