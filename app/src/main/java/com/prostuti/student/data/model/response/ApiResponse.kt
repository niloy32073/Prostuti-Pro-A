package com.prostuti.student.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Non-paged endpoints (e.g. exams/{id}/questions/with-options) send these as null.
@Serializable
data class MetaData(
    @SerialName("current_page")
    val currentPage: Int? = null,

    @SerialName("page_limit")
    val pageLimit: Int? = null,

    @SerialName("total_items")
    val totalItems: Int? = null,

    @SerialName("total_pages")
    val totalPages: Int? = null
)

@Serializable
data class ErrorDetail(
    val code: String,
    val details: String? = null // Change type if details returns a complex object
)

@Serializable
data class BaseResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val error: ErrorDetail? = null,
    @SerialName("meta_data")
    val metaData: MetaData? = null
)

@Serializable
sealed class ApiResponse<out T> {

    @Serializable
    data class Success<T>(
        val data: T,
        val message: String = "",
        val metaData: MetaData? = null
    ) : ApiResponse<T>()

    @Serializable
    data class Failure(
        val message: String,
        val errorCode: String,
        val details: String? = null
    ) : ApiResponse<Nothing>()
}
