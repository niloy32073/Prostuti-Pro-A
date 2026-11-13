package com.prostuti.student.data.model.response

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/** One MCQ with its answer, from `GET questions/mcq/{id}` — shown on the bookmark history screen. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class McqDetail(
    @SerialName("id")
    @JsonNames("mcq_id")
    val id: String = "",
    @SerialName("question_text")
    val questionText: String? = null,
    @SerialName("question_image_link")
    val questionImageLink: String? = null,
    @SerialName("passage_id")
    val passageId: String? = null,
    @SerialName("options")
    val options: List<McqDetailOption> = emptyList(),
    @SerialName("explanation")
    val explanation: String? = null
)
