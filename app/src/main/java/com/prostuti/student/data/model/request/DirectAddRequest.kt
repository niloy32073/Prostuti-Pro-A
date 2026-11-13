package com.prostuti.student.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** Body of `POST /groups/{id}/members` — a registered email, or a phone number as `+<country code>…`. */
@Serializable
data class DirectAddRequest(@SerialName("identifier") val identifier: String)
