package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Acknowledgement of a group action with nothing else to return (leave, delete, remove member). */
@Serializable
data class GroupActionAck(@SerialName("group_id") val groupId: String)
