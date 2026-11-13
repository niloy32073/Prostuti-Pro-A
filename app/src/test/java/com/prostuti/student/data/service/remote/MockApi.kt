package com.prostuti.student.data.service.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** One fake server for the groups/routines/notifications API tests: records the last request and
 * answers every call with [body]. Uses the same Json settings as the app's HttpClient. */
class MockApi(private val body: String, private val status: HttpStatusCode = HttpStatusCode.OK) {
    var method: HttpMethod? = null
    var url: Url? = null
    var requestBody: String? = null

    val client = HttpClient(MockEngine { request ->
        method = request.method
        url = request.url
        requestBody = request.body.toByteArray().decodeToString()
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        defaultRequest { url("https://api.test/api/v1/") }
        install(ContentNegotiation) { json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }) }
    }

    val path: String get() = url!!.encodedPath
    fun param(name: String): String? = url!!.parameters[name]
    fun jsonBody(): JsonObject = Json.parseToJsonElement(requestBody!!).jsonObject

    companion object {
        fun ok(data: String, meta: String = "null") = """{ "success": true, "message": "ok", "data": $data, "error": null, "meta_data": $meta }"""
        fun error(code: String) = """{ "success": false, "message": "server says $code", "data": null, "error": { "code": "$code", "details": null } }"""
    }
}
