package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchPassageData(
    @SerialName("id")
    val id: String,
    @SerialName("passage_text")
    val passageText: String? = null,
    @SerialName("passage_image")
    val passageImage: String? = null
)
