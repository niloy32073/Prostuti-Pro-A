package com.prostuti.student.domain.group

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.data.repository.GroupRepository
import com.prostuti.student.util.getNormalizedPhone

/**
 * Adds a registered user to a group by email or phone (admin). An email is trimmed and
 * lower-cased; anything else goes through [getNormalizedPhone], so `01XXXXXXXXX` is sent as
 * `+8801XXXXXXXXX` — the API only accepts phones in `+<country code>` form.
 */
class AddGroupMemberUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String, identifier: String): ApiResponse<StudyGroupMember> =
        repository.addMember(groupId, normalizeIdentifier(identifier))

    internal fun normalizeIdentifier(identifier: String): String {
        val trimmed = identifier.trim()
        return if ('@' in trimmed) trimmed.lowercase() else getNormalizedPhone(trimmed)
    }
}
