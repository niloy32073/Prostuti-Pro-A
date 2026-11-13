package com.prostuti.student.data.service.remote

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** `page` / `limit` query parameters of the paged list endpoints. */
internal fun HttpRequestBuilder.paging(page: Int, limit: Int) {
    parameter("page", page)
    parameter("limit", limit)
}

/** A JSON request body. */
internal inline fun <reified T> HttpRequestBuilder.json(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}
