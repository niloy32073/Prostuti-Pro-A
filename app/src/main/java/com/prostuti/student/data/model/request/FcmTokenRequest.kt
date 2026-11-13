package com.prostuti.student.data.model.request

import kotlinx.serialization.Serializable

/** Body of `PUT /notifications/fcm-token`. `token = null` clears the device (sent as `{"token": null}` on logout). */
@Serializable
data class FcmTokenRequest(val token: String?)
