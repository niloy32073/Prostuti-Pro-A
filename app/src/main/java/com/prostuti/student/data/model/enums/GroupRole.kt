package com.prostuti.student.data.model.enums

import com.prostuti.student.util.FallbackEnumSerializer
import kotlinx.serialization.Serializable


/** A member's standing inside one specific group — not a global user role. The same person can
 * be ADMIN in one group and MEMBER in another. Old server roles (MODERATOR, TEACHER) or any other
 * unknown value read as [MEMBER], never as admin. */
@Serializable(with = GroupRoleSerializer::class)
enum class GroupRole {
    ADMIN, MEMBER
}

object GroupRoleSerializer : FallbackEnumSerializer<GroupRole>("GroupRole", GroupRole.entries, GroupRole.MEMBER)
