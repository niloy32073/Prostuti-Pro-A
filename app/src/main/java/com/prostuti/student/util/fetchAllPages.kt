package com.prostuti.student.util

import com.prostuti.student.data.model.response.ApiResponse

/**
 * Reads a paged list endpoint from page 1 until `meta_data.total_pages` (or an empty or short page),
 * at most [maxPages] pages. Any failing page fails the whole call, so a screen never shows a
 * silently truncated list.
 */
suspend fun <T> fetchAllPages(
    limit: Int,
    maxPages: Int = 20,
    fetch: suspend (page: Int, limit: Int) -> ApiResponse<List<T>>,
): ApiResponse<List<T>> {
    val all = mutableListOf<T>()
    var page = 1
    while (page <= maxPages) {
        when (val r = fetch(page, limit)) {
            is ApiResponse.Failure -> return r
            is ApiResponse.Success -> {
                all += r.data
                val totalPages = r.metaData?.totalPages
                val isLast = if (totalPages != null) page >= totalPages else r.data.size < limit
                if (isLast || r.data.isEmpty()) return ApiResponse.Success(all, r.message, r.metaData)
            }
        }
        page++
    }
    return ApiResponse.Success(all)
}
