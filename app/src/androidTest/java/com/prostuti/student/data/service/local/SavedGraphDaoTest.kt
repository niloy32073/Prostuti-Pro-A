package com.prostuti.student.data.service.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.data.repository.SavedGraphRepositoryImpl
import com.prostuti.student.util.TimeProvider
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
class SavedGraphDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SavedGraphDao
    private var now = 1_000L
    private lateinit var repo: SavedGraphRepositoryImpl

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.savedGraphDao()
        repo = SavedGraphRepositoryImpl(dao, object : TimeProvider { override fun nowEpochMillis() = now })
    }

    @After fun teardown() { db.close() }

    @Test fun firstSaveCreatesLaterSavesOverwrite() = runBlocking {
        val id = repo.save(null, "Parabola", "{a}", "y = x^2", false)
        assertTrue(id > 0)
        now = 2_000
        val again = repo.save(id, "Parabola", "{b}", "y = x^2 + 1", false)
        assertEquals(id, again)
        val row = dao.getById(id)!!
        assertEquals("{b}", row.content)
        assertEquals(2_000, row.updatedAt)
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test fun listIsNewestFirstAndRenameDeleteWork() = runBlocking {
        val a = repo.save(null, "A", "{}", "", false)
        now = 5_000
        val b = repo.save(null, "B", "{}", "", true)
        assertEquals(listOf(b, a), dao.observeAll().first().map { it.id })
        repo.rename(a, "Renamed")
        assertEquals("Renamed", dao.getById(a)!!.name)
        repo.delete(b)
        assertNull(dao.getById(b))
        assertEquals(listOf(a), dao.observeAll().first().map { it.id })
    }
}
