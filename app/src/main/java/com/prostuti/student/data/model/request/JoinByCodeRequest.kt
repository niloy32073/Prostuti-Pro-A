package com.prostuti.student.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Body of `POST /groups/join` — a 4-20 character group code (case-insensitive). */
@Serializable
data class JoinByCodeRequest(@SerialName("code") val code: String)
