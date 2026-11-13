package com.prostuti.student.data.service.remote

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.Chapter
import com.prostuti.student.data.model.response.Subject
import com.prostuti.student.data.model.response.Topic
import com.prostuti.student.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter

/**
 * The curriculum lists come back in pages (server default 20–50 a page), so each list call here
 * walks every page and returns the whole list — otherwise subjects past the first page would never
 * show up in the builder, practice or history.
 */
class CurriculumApiService(private val client: HttpClient) {
    suspend fun fetchSubjects(): ApiResponse<List<Subject>> =
        allPages { page -> safeApiCall { client.get("curriculum/subjects") { paged(page) } } }

    suspend fun fetchChapters(subjectId: String): ApiResponse<List<Chapter>> =
        allPages { page -> safeApiCall { client.get("curriculum/subjects/$subjectId/chapters") { paged(page) } } }

    suspend fun fetchTopics(chapterId: String): ApiResponse<List<Topic>> =
        allPages { page -> safeApiCall { client.get("curriculum/chapters/$chapterId/topics") { paged(page) } } }

    suspend fun fetchChapter(chapterId: String): ApiResponse<Chapter> =
        safeApiCall { client.get("curriculum/chapters/$chapterId") }

    suspend fun fetchTopic(topicId: String): ApiResponse<Topic> =
        safeApiCall { client.get("curriculum/topics/$topicId") }

    private fun HttpRequestBuilder.paged(page: Int) {
        parameter("page", page)
        parameter("limit", PAGE_SIZE)
    }

    /**
     * Fetches page 1, 2, … until `total_pages` (or, without it, a short page). Any failed page fails
     * the whole list, so a cache is never replaced by half of it.
     */
    private suspend fun <T> allPages(get: suspend (page: Int) -> ApiResponse<List<T>>): ApiResponse<List<T>> {
        val items = mutableListOf<T>()
        var page = 1
        while (true) {
            when (val r = get(page)) {
                is ApiResponse.Failure -> return r
                is ApiResponse.Success -> {
                    items += r.data
                    val totalPages = r.metaData?.totalPages
                    val more = if (totalPages != null) page < totalPages else r.data.size >= PAGE_SIZE
                    if (!more || page >= MAX_PAGES) return ApiResponse.Success(items, r.message, r.metaData)
                }
            }
            page++
        }
    }

    companion object {
        /** The list endpoints' largest page. */
        const val PAGE_SIZE = 100
        /** A guard against a server that never says it's done (10 000 items). */
        const val MAX_PAGES = 100
    }
}
