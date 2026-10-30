package com.prostuti.student.util

import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.utils.EmptyContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import timber.log.Timber

/**
 * Logs every outgoing request (method, full URL, body) in the same
 * "API ... | URL: ..." style as the response logs in [safeApiCall].
 * Install it only in debug builds. Headers are never logged, and
 * password/token fields in JSON bodies are masked.
 */
val RequestLogging = createClientPlugin("RequestLogging") {
    on(SendingRequest) { request, content ->
        val url = request.url.buildString()
        Timber.d("API Request | ${request.method.value} | URL: $url | Body: ${describeBody(content)}")
    }
}

private val SECRET_FIELDS = Regex(
    """"(password|new_password|token|reset_token|refresh_token|access_token)"\s*:\s*"(?:[^"\\]|\\.)*""""
)

private fun maskSecrets(body: String): String =
    SECRET_FIELDS.replace(body) { "\"${it.groupValues[1]}\":\"***\"" }

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
