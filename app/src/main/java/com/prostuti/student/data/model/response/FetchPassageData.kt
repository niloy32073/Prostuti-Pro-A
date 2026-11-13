package com.prostuti.student.data.model.response

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class FetchPassageData(
    @SerialName("id")
    val id: String,
    @SerialName("passage_text")
    val passageText: String? = null,
    // The passage API sends "passage_image_link"; the older name is still accepted.
    @SerialName("passage_image_link")
    @JsonNames("passage_image")
    val passageImage: String? = null
)
