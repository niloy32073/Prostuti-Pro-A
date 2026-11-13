package com.prostuti.student.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.data.model.response.CurriculumLookup
import com.prostuti.student.data.service.local.AppDatabase
import com.prostuti.student.data.service.remote.CurriculumApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A group member's empty cache filled from the server, against a real Room database: topics need
 * their chapter and chapters their subject (foreign keys). Saving a topic first used to crash the app.
 */
@RunWith(AndroidJUnit4::class)
class CurriculumNamesDbTest {
    private lateinit var db: AppDatabase
    private var subjectsDown = false

    private fun subject(id: String) = """{ "id": "$id", "name_bn": "পদার্থবিজ্ঞান", "name_en": "Physics", "created_at": "2026-01-01T00:00:00", "created_by": "a" }"""
    private fun chapter(id: String, name: String) =
        """{ "id": "$id", "subject_id": "phy", "chapter_name_bn": "$name", "chapter_name_en": "$id", "order": 1, "created_at": "2026-01-01T00:00:00", "created_by": "a" }"""
    private fun topic(id: String, chapterId: String, name: String) =
        """{ "id": "$id", "chapter_id": "$chapterId", "name_bn": "$name", "name_en": "$id", "order": 1, "created_at": "2026-01-01T00:00:00", "created_by": "a" }"""
    private fun ok(data: String) = """{ "success": true, "message": "ok", "data": $data, "error": null, "meta_data": null }"""

    private val server = HttpClient(MockEngine { request ->
        val path = request.url.encodedPath.removePrefix("/api/v1/")
        val body = when {
            path == "curriculum/subjects" && subjectsDown -> null
            path == "curriculum/subjects" -> ok("[${subject("phy")}]")
            path == "curriculum/subjects/phy/chapters" -> ok("[${chapter("vec", "ভেক্টর")}, ${chapter("motion", "গতি")}]")
            path == "curriculum/chapters/vec/topics" -> ok("[${topic("v1", "vec", "ভেক্টরের যোগ")}, ${topic("v2", "vec", "ডট গুণন")}]")
            path == "curriculum/chapters/motion" -> ok(chapter("motion", "গতি"))
            path == "curriculum/topics/m1" -> ok(topic("m1", "motion", "প্রাসের গতি"))
            else -> null
        }
        if (body == null) respond("""{ "success": false, "message": "down", "data": null, "error": { "code": "DOWN" } }""", HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.ContentType, "application/json"))
        else respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        defaultRequest { url("https://api.test/api/v1/") }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
    }

    private lateinit var repo: CurriculumRepositoryImpl

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        repo = CurriculumRepositoryImpl(CurriculumApiService(server), db.subjectDao(), db.chapterDao(), db.topicDao())
    }

    @After fun teardown() { db.close() }

    @Test fun anEmptyCacheIsFilledParentsFirstWithoutCrashing() = runBlocking {
        repo.fillNames(
            CurriculumLookup(
                subjectIds = setOf("phy"),
                chaptersBySubject = mapOf("phy" to setOf("vec")),
                topicsByChapter = mapOf("vec" to setOf("v1", "v2")),
                looseTopicIds = setOf("m1"),
            )
        )
        val names = repo.cachedNames(setOf("phy"), setOf("vec", "motion"), setOf("v1", "v2", "m1"))
        assertEquals("পদার্থবিজ্ঞান", names.subjects["phy"])
        assertEquals(setOf("ভেক্টর", "গতি"), names.chapters.values.toSet())
        assertEquals(setOf("ভেক্টরের যোগ", "ডট গুণন", "প্রাসের গতি"), names.topics.values.toSet())
        assertEquals("motion", names.topicChapters["m1"])
    }

    @Test fun withoutTheSubjectNothingBelowItIsSavedAndNothingCrashes() = runBlocking {
        subjectsDown = true
        repo.fillNames(CurriculumLookup(subjectIds = setOf("phy"), chaptersBySubject = mapOf("phy" to setOf("vec")), topicsByChapter = mapOf("vec" to setOf("v1"))))
        val names = repo.cachedNames(setOf("phy"), setOf("vec"), setOf("v1"))
        assertTrue(names.subjects.isEmpty() && names.chapters.isEmpty() && names.topics.isEmpty())
    }
}
