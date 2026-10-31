package com.prostuti.student.data.service.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prostuti.student.data.repository.fake.FakeUserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurrentUserDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: CurrentUserDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.currentUserDao()
    }

    @After fun teardown() { db.close() }

    @Test fun replaceStoresTheProfileWithItsScopes() = runBlocking {
        val me = FakeUserRepository.me()
        dao.replace(me.toEntity())

        assertEquals(me, dao.get()?.toMeData())
        assertEquals(listOf("curriculum:read", "exam:attempt", "exam:read"), dao.observe().first()?.scopes)
    }

    @Test fun replaceWithAnotherUserKeepsOneRow() = runBlocking {
        dao.replace(FakeUserRepository.me().toEntity())
        dao.replace(FakeUserRepository.me(firstName = "Rahim").toEntity().copy(userId = "other"))

        assertEquals("other", dao.get()?.userId)
        assertEquals("Rahim", dao.observe().first()?.firstName)
    }

    @Test fun deleteAllClearsTheCache() = runBlocking {
        dao.replace(FakeUserRepository.me().toEntity())
        dao.deleteAll()

        assertNull(dao.get())
    }
}
