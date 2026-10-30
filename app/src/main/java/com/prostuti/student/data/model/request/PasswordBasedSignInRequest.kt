package com.prostuti.student.data.model.request

import com.prostuti.student.data.model.enums.DeviceCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PasswordBasedSignInRequest(
    val identifier: String,
    val password: String,
    @SerialName("device_category") val deviceCategory: DeviceCategory,
    @SerialName("device_name") val deviceName: String
)
