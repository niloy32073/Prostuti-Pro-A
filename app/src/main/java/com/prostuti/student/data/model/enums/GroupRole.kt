package com.prostuti.student.data.model.enums

import kotlinx.serialization.Serializable

/** A member's standing inside one specific group — not a global user role. The same person can
 * be ADMIN in one group and MEMBER in another. */
@Serializable
enum class GroupRole {
    ADMIN, MEMBER
}
