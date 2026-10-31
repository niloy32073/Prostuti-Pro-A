package com.prostuti.student.util

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import timber.log.Timber

/**
 * Debug-only HTTP logging, all under the "ApiLog" tag so one logcat filter shows a whole call:
 *  1. `API Request`  — method, URL and the serialized request body ([RequestLogging])
 *  2. `API Response` — status, URL and the raw JSON the server sent
 *  3. `API Decoded`  — the Kotlin object kotlinx.serialization produced from it
 * Token/password fields are masked; other personal data is not, so never enable this in release.
 */
object ApiLog {
    const val TAG = "ApiLog"

    private val prettyJson = Json { prettyPrint = true }

    private val SECRET_FIELDS = Regex(
        """"(password|new_password|token|reset_token|refresh_token|access_token)"\s*:\s*"(?:[^"\\]|\\.)*""""
    )

    fun maskSecrets(body: String): String =
        SECRET_FIELDS.replace(body) { "\"${it.groupValues[1]}\":\"***\"" }

    fun request(method: String, url: String, body: String) {
        Timber.tag(TAG).d("API Request | %s | URL: %s\nBody: %s", method, url, body)
    }

    /** Reads the body as text; Ktor keeps the body in memory, so it can still be decoded afterwards. */
    suspend fun response(response: HttpResponse) {
        val raw = try {
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "<unreadable body: ${e.message}>"
        }
        Timber.tag(TAG).d(
            "API Response | %s | %s %s\nJson: %s",
            response.status.value,
            response.call.request.method.value,
            response.call.request.url,
            maskSecrets(pretty(raw))
        )
    }

    fun decoded(url: String, value: Any?) {
        Timber.tag(TAG).d("API Decoded | URL: %s\nData: %s", url, maskSecrets(value.toString()))
    }

    private fun pretty(raw: String): String = try {
        prettyJson.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(raw))
    } catch (e: Exception) {
        raw // not JSON (HTML error page, empty body, …) — show as-is
    }
}
