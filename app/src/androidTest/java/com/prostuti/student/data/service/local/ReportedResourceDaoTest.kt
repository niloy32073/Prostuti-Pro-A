package com.prostuti.student.data.service.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.prostuti.student.data.model.enums.ReportResourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportedResourceDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ReportedResourceDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.reportedResourceDao()
    }

    @After fun teardown() { db.close() }

    private fun row(id: String, type: ReportResourceType = ReportResourceType.MCQ) =
        ReportedResourceEntity(type, id, "r-$id", "reason", reportedAt = 1L)

    @Test fun reportedIdsAreKeptPerType() = runBlocking {
        dao.insert(row("m1"))
        dao.insert(row("d1", ReportResourceType.DESCRIPTIVE_QUESTION))

        assertEquals(listOf("m1"), dao.observeIdsByType(ReportResourceType.MCQ).first())
        assertTrue(dao.exists(ReportResourceType.MCQ, "m1"))
        assertFalse(dao.exists(ReportResourceType.MCQ, "d1"))
    }

    @Test fun deleteAllForgetsEveryReport() = runBlocking {
        dao.insert(row("m1"))
        dao.deleteAll()

        assertTrue(dao.observeIdsByType(ReportResourceType.MCQ).first().isEmpty())
    }
}
