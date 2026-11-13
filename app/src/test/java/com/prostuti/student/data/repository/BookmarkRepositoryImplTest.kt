package com.prostuti.student.data.repository

import com.prostuti.student.data.model.enums.BookmarkResourceType
import com.prostuti.student.data.model.enums.BookmarkResourceType.EBOOK
import com.prostuti.student.data.model.enums.BookmarkResourceType.MCQ
import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.repository.fake.FakeBookmarkDao
import com.prostuti.student.data.repository.fake.FakeDataStoreStorageRepository
import com.prostuti.student.data.service.local.BookmarkEntity
import com.prostuti.student.data.service.remote.BookmarkApiService
import com.prostuti.student.dataStore.PrefKeys
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class BookmarkRepositoryImplTest {

    private val requests = mutableListOf<HttpRequestData>()
    private val dao = FakeBookmarkDao()
    private val storage = FakeDataStoreStorageRepository()

    /** [handler] answers each request; same JSON settings as the app's HttpClient. A new call = a new app run. */
    private fun repo(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ) = BookmarkRepositoryImpl(
        BookmarkApiService(
            HttpClient(MockEngine { request -> requests += request; handler(request) }) {
                defaultRequest { url("https://api.test/api/v1/") }
                install(ContentNegotiation) { json(Json { isLenient = true; ignoreUnknownKeys = true }) }
            }
        ),
        dao,
        storage
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun bookmarkJson(id: String, bookmarkId: String = "b-$id", type: String = "MCQ") =
        """{ "id": "$bookmarkId", "user_id": "u1", "resource_type": "$type", "resource_id": "$id",
             "page_no": null, "comments": null, "created_at": "2026-10-01T10:00:00Z" }"""

    private fun listJson(vararg items: String, page: Int = 1, totalPages: Int = 1) =
        """{ "success": true, "message": "ok", "data": [${items.joinToString()}],
             "meta_data": { "current_page": $page, "page_limit": 100, "total_items": ${items.size}, "total_pages": $totalPages } }"""

    private fun cached(id: String, type: BookmarkResourceType = MCQ) =
        BookmarkEntity(type, id, "b-$id", createdAt = "2026-10-01T10:00:00Z")

    private suspend fun syncedFlag() = storage.getBoolean(PrefKeys.BOOKMARKS_SYNCED).first()

    // ── App-open check / synced flag ──

    @Test fun `first open downloads the list and sets the synced flag`() = runTest {
        val repo = repo { json(listJson(bookmarkJson("m1"))) }

        assertTrue(repo.syncIfNeeded() is ApiResponse.Success)

        assertEquals(1, requests.size)
        assertEquals("b-m1", dao.get(MCQ, "m1")?.bookmarkId)
        assertEquals(true, syncedFlag())
    }

    @Test fun `an empty server list still sets the flag, so the next app run makes no call`() = runTest {
        repo { json(listJson()) }.syncIfNeeded()
        assertEquals(true, syncedFlag())

        val nextRun = repo { error("must not call the server") }
        nextRun.syncIfNeeded()
        nextRun.observeBookmarkedIds(MCQ).first()

        assertEquals(1, requests.size)
    }

    @Test fun `after syncing, a new app run reads bookmarks from Room only`() = runTest {
        repo { json(listJson(bookmarkJson("m1"))) }.syncIfNeeded()

        val nextRun = repo { error("must not call the server") }
        assertEquals(setOf("m1"), nextRun.observeBookmarkedIds(MCQ).first())
        assertEquals(1, requests.size)
    }

    @Test fun `a failed first sync leaves the flag unset and is retried on the next read`() = runTest {
        var online = false
        val repo = repo { if (online) json(listJson(bookmarkJson("m1"))) else throw IOException("offline") }

        assertTrue(repo.syncIfNeeded() is ApiResponse.Failure)
        assertEquals(null, syncedFlag())

        online = true
        assertEquals(setOf("m1"), repo.observeBookmarkedIds(MCQ).first())
        assertEquals(true, syncedFlag())
    }

    @Test fun `refresh downloads again even when synced`() = runTest {
        val repo = repo { json(listJson(bookmarkJson("m1"))) }
        repo.observeBookmarkedIds(MCQ).first()
        repo.observeBookmarks(MCQ, refresh = false).first()
        assertEquals(1, requests.size)

        repo.observeBookmarks(MCQ, refresh = true).first()
        assertEquals(2, requests.size)
    }

    @Test fun `clear empties the cache, resets the flag and the next read downloads again`() = runTest {
        val repo = repo { json(listJson(bookmarkJson("m1"))) }
        repo.syncIfNeeded()

        repo.clearBookmarks()
        assertNull(dao.get(MCQ, "m1"))
        assertEquals(false, syncedFlag())

        repo.observeBookmarkedIds(MCQ).first()
        assertEquals(2, requests.size)
    }

    // ── Every type is cached; each read sees its own type ──

    @Test fun `sync reads every page of every type and skips unknown types`() = runTest {
        val repo = repo { request ->
            assertNull(request.url.parameters["resource_type"])
            when (request.url.parameters["page"]) {
                "1" -> json(listJson(bookmarkJson("m1"), bookmarkJson("e1", type = "EBOOK"), page = 1, totalPages = 2))
                else -> json(listJson(bookmarkJson("m2"), bookmarkJson("x1", type = "VIDEO"), page = 2, totalPages = 2))
            }
        }

        assertEquals(setOf("m1", "m2"), repo.observeBookmarkedIds(MCQ).first())
        assertEquals(setOf("e1"), repo.observeBookmarkedIds(EBOOK).first())
        assertEquals(2, requests.size)
        assertEquals(3, dao.rows.value.size)
    }

    // ── Add: server first, then Room ──

    @Test fun `add posts the bookmark with its type and caches it`() = runTest {
        storage.putBoolean(PrefKeys.BOOKMARKS_SYNCED, true)
        val repo = repo {
            json("""{ "success": true, "message": "Bookmark created successfully.", "data": ${bookmarkJson("e1", type = "EBOOK")} }""", HttpStatusCode.Created)
        }

        val result = repo.addBookmark(EBOOK, "e1", pageNo = 12)

        assertTrue(result is ApiResponse.Success)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/learning/bookmarks", request.url.encodedPath)
        val body = (request.body as TextContent).text
        assertTrue(body.contains("\"resource_type\":\"EBOOK\""))
        assertTrue(body.contains("\"resource_id\":\"e1\""))
        assertTrue(body.contains("\"page_no\":12"))
        assertEquals("b-e1", dao.get(EBOOK, "e1")?.bookmarkId)
    }

    @Test fun `add of something already cached makes no call`() = runTest {
        dao.insert(cached("m1"))
        val repo = repo { error("must not call the server") }

        assertTrue(repo.addBookmark(MCQ, "m1") is ApiResponse.Success)
        assertTrue(requests.isEmpty())
    }

    @Test fun `add of an mcq already bookmarked elsewhere (409) syncs and succeeds`() = runTest {
        val repo = repo { request ->
            if (request.method == HttpMethod.Post) {
                json("""{ "success": false, "message": "Already bookmarked", "error": { "code": "CONFLICT" } }""", HttpStatusCode.Conflict)
            } else {
                json(listJson(bookmarkJson("m1", bookmarkId = "server-b1")))
            }
        }

        assertTrue(repo.addBookmark(MCQ, "m1") is ApiResponse.Success)
        assertEquals("server-b1", dao.get(MCQ, "m1")?.bookmarkId)
    }

    @Test fun `add fails when the network is down and caches nothing`() = runTest {
        val repo = repo { throw IOException("offline") }

        assertTrue(repo.addBookmark(MCQ, "m1") is ApiResponse.Failure)
        assertNull(dao.get(MCQ, "m1"))
    }

    // ── Remove: server first, then Room ──

    @Test fun `remove deletes by bookmark id and accepts a null data body`() = runTest {
        dao.insert(cached("m1"))
        val repo = repo { json("""{ "success": true, "message": "Bookmark removed successfully.", "data": null }""") }

        assertTrue(repo.removeBookmark(MCQ, "m1") is ApiResponse.Success)
        assertEquals(HttpMethod.Delete, requests.single().method)
        assertEquals("/api/v1/learning/bookmarks/b-m1", requests.single().url.encodedPath)
        assertNull(dao.get(MCQ, "m1"))
    }

    @Test fun `removing the last bookmark keeps the synced flag, so the empty list is trusted`() = runTest {
        storage.putBoolean(PrefKeys.BOOKMARKS_SYNCED, true)
        dao.insert(cached("m1"))
        repo { json("""{ "success": true, "message": "ok", "data": null }""") }.removeBookmark(MCQ, "m1")

        val nextRun = repo { error("must not call the server") }
        assertTrue(nextRun.observeBookmarkedIds(MCQ).first().isEmpty())
        assertEquals(1, requests.size)
    }

    @Test fun `remove of a bookmark already gone (404) syncs and succeeds`() = runTest {
        dao.insert(cached("m1"))
        val repo = repo { request ->
            if (request.method == HttpMethod.Delete) {
                json("""{ "success": false, "message": "Not found", "error": { "code": "NOT_FOUND" } }""", HttpStatusCode.NotFound)
            } else {
                json(listJson())
            }
        }

        assertTrue(repo.removeBookmark(MCQ, "m1") is ApiResponse.Success)
        assertNull(dao.get(MCQ, "m1"))
    }

    @Test fun `remove that fails keeps the cached bookmark`() = runTest {
        dao.insert(cached("m1"))
        val repo = repo { throw IOException("offline") }

        assertTrue(repo.removeBookmark(MCQ, "m1") is ApiResponse.Failure)
        assertEquals("b-m1", dao.get(MCQ, "m1")?.bookmarkId)
    }

    // ── Bookmark list screen ──

    @Test fun `a failed first load reports the error and shows no list`() = runTest {
        val repo = repo { throw IOException("offline") }

        val emissions = mutableListOf<ApiResponse<List<BookmarkEntity>>>()
        repo.observeBookmarks(MCQ, refresh = false).collect { emissions += it }

        assertTrue(emissions.single() is ApiResponse.Failure)
    }

    @Test fun `a failed refresh still shows the cached list`() = runTest {
        var online = true
        val repo = repo { if (online) json(listJson(bookmarkJson("m1"))) else throw IOException("offline") }
        repo.syncIfNeeded()

        online = false
        val (error, list) = repo.observeBookmarks(MCQ, refresh = true).take(2).toList()

        assertTrue(error is ApiResponse.Failure)
        assertEquals(listOf("m1"), (list as ApiResponse.Success).data.map { it.resourceId })
    }
}
