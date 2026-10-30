package com.prostuti.student.data.model.response

import com.prostuti.student.data.model.enums.GroupRole
import kotlinx.serialization.Serializable

/** One row of "আমার গ্রুপ" (GroupList). [myRole] is per-group, never global — the same student
 * can be [GroupRole.ADMIN] in one group and [GroupRole.MEMBER] in another. */
@Serializable
data class Group(
    val id: String,
    val nameBn: String,
    val memberCount: Int,
    val myRole: GroupRole,
    val joinCode: String
)

@Serializable
data class GroupMember(
    val id: String,
    val name: String,
    val initial: String,
    val isAdmin: Boolean,
    val isMe: Boolean = false
)

@Serializable
data class GroupPastExam(
    val examId: String,
    val titleBn: String,
    val dateLabelBn: String,
    val submittedCount: Int,
    val totalCount: Int,
    val averagePercent: Int,
    /** null = the student did not take this one. */
    val myScoreLabel: String?
)
