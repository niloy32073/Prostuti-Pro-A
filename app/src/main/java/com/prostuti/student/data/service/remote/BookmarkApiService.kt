package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.request.BookmarkCreateRequest
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Bookmark
import com.prostuti.student.util.safeApiCall
import com.prostuti.student.util.safeApiCallNoData
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** `learning/bookmarks` — one endpoint for every resource type (MCQ, questions, e-books). */
class BookmarkApiService(private val client: HttpClient) {

    /** 409 when this resource is already bookmarked. */
    suspend fun createBookmark(request: BookmarkCreateRequest): ApiResponse<Bookmark> = safeApiCall {
        client.post("learning/bookmarks") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /** One page of the user's bookmarks of every type, newest first. [limit] is 1–100. */
    suspend fun listBookmarks(page: Int, limit: Int): ApiResponse<List<Bookmark>> = safeApiCall {
        client.get("learning/bookmarks") {
            parameter("page", page)
            parameter("limit", limit)
        }
    }

    /** 404 when the bookmark is already gone. */
    suspend fun deleteBookmark(bookmarkId: String): ApiResponse<Unit> = safeApiCallNoData {
        client.delete("learning/bookmarks/$bookmarkId")
    }
}
