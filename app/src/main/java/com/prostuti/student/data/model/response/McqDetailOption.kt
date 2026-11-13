package com.prostuti.student.data.model.response

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class McqDetailOption(
    @SerialName("id")
    val id: String,
    @SerialName("option_text")
    val optionText: String? = null,
    @SerialName("option_image")
    @JsonNames("option_image_link")
    val optionImageLink: String? = null,
    @SerialName("order")
    val order: Int = 0,
    @SerialName("is_correct")
    val isCorrect: Boolean = false
)
