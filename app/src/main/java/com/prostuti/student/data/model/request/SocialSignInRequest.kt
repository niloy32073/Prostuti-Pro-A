package com.prostuti.student.data.model.request

import kotlinx.serialization.Serializable

@Serializable
data class SocialSignInRequest(
    val provider: String, // "GOOGLE" | "FACEBOOK"
    val token: String
)
