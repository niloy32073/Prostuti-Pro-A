package com.prostuti.student.util

import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.utils.EmptyContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent

/**
 * Logs every outgoing request (method, full URL, serialized body) through [ApiLog], next to the
 * response and decoded-object logs that [safeApiCall] writes. Install it only in debug builds.
 * Headers are never logged, and password/token fields in JSON bodies are masked.
 */
val RequestLogging = createClientPlugin("RequestLogging") {
    on(SendingRequest) { request, content ->
        ApiLog.request(request.method.value, request.url.buildString(), describeBody(content))
    }
}

private fun maskSecrets(body: String): String = ApiLog.maskSecrets(body)

private fun describeBody(content: OutgoingContent): String = when (content) {
    is EmptyContent, is OutgoingContent.NoContent -> "<empty>"
    is TextContent -> maskSecrets(content.text)
    is OutgoingContent.ByteArrayContent -> {
        val bytes = content.bytes()
        val type = content.contentType
        if (type != null && (type.contentType == "text" || type.contentSubtype.contains("json"))) {
            maskSecrets(bytes.decodeToString())
        } else {
            "<${bytes.size} bytes, $type>"
        }
    }
    else -> "<streamed ${content::class.simpleName}, ${content.contentType}>"
}
