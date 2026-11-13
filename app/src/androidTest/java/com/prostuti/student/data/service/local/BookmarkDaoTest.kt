package com.prostuti.student.data.service.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.data.model.enums.BookmarkResourceType.EBOOK
import com.prostuti.student.data.model.enums.BookmarkResourceType.MCQ
import com.prostuti.student.data.model.enums.BookmarkResourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: BookmarkDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.bookmarkDao()
    }

    @After fun teardown() { db.close() }

    private fun row(id: String, createdAt: String, type: BookmarkResourceType = MCQ) =
        BookmarkEntity(type, id, "b-$type-$id", createdAt = createdAt)

    @Test fun observeByTypeListsNewestFirstAndOnlyThatType() = runBlocking {
        dao.insertAll(listOf(row("old", "2026-01-01T00:00:00Z"), row("new", "2026-10-01T00:00:00Z"), row("book", "2026-11-01T00:00:00Z", EBOOK)))

        assertEquals(listOf("new", "old"), dao.observeByType(MCQ).first().map { it.resourceId })
        assertEquals(setOf("new", "old"), dao.observeIdsByType(MCQ).first().toSet())
        assertEquals(listOf("book"), dao.observeIdsByType(EBOOK).first())
    }

    @Test fun sameIdUnderTwoTypesIsTwoRows() = runBlocking {
        dao.insert(row("x", "2026-01-01T00:00:00Z", MCQ))
        dao.insert(row("x", "2026-01-01T00:00:00Z", EBOOK))

        assertEquals("b-MCQ-x", dao.get(MCQ, "x")?.bookmarkId)
        assertEquals("b-EBOOK-x", dao.get(EBOOK, "x")?.bookmarkId)
    }

    @Test fun insertingTheSameResourceAgainKeepsOneRow() = runBlocking {
        dao.insert(row("m1", "2026-01-01T00:00:00Z"))
        dao.insert(row("m1", "2026-01-01T00:00:00Z").copy(bookmarkId = "b-new"))

        assertEquals(1, dao.observeByType(MCQ).first().size)
        assertEquals("b-new", dao.get(MCQ, "m1")?.bookmarkId)
    }

    @Test fun replaceAllSwapsTheWholeList() = runBlocking {
        dao.insert(row("gone", "2026-01-01T00:00:00Z"))
        dao.replaceAll(listOf(row("m1", "2026-02-01T00:00:00Z")))

        assertNull(dao.get(MCQ, "gone"))
        assertEquals(listOf("m1"), dao.observeIdsByType(MCQ).first())
    }

    @Test fun deleteOneAndDeleteAll() = runBlocking {
        dao.insertAll(listOf(row("m1", "2026-01-01T00:00:00Z"), row("m2", "2026-01-02T00:00:00Z")))

        dao.delete(MCQ, "m1")
        assertEquals(listOf("m2"), dao.observeIdsByType(MCQ).first())

        dao.deleteAll()
        assertTrue(dao.observeByType(MCQ).first().isEmpty())
    }
}
