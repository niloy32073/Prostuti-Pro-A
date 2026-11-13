package com.prostuti.student.domain.group

import com.prostuti.student.data.model.enums.GroupRole
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.StudyGroupMember
import com.prostuti.student.data.repository.GroupRepository

/** Promotes a member to admin or demotes an admin (admin). */
class SetGroupMemberRoleUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String, userId: String, role: GroupRole): ApiResponse<StudyGroupMember> = repository.setMemberRole(groupId, userId, role)
}
