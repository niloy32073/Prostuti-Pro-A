package com.prostuti.student.data.model.enums

import com.prostuti.student.util.FallbackEnumSerializer
import kotlinx.serialization.Serializable


/** State of a group join request (also the `status` filter of the admin's request list). */
@Serializable(with = JoinRequestStatusSerializer::class)
enum class JoinRequestStatus {
    PENDING, APPROVED, REJECTED
}

object JoinRequestStatusSerializer :
    FallbackEnumSerializer<JoinRequestStatus>("JoinRequestStatus", JoinRequestStatus.entries, JoinRequestStatus.PENDING)
