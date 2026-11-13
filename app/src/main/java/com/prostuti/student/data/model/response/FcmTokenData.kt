package com.prostuti.student.data.model.response

import kotlinx.serialization.Serializable

/** `registered` is true when a token is now stored, false when it was cleared. */
@Serializable
data class FcmTokenData(val registered: Boolean)
