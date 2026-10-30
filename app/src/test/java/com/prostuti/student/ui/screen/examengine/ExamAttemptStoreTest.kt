package com.prostuti.student.ui.screen.examengine

import com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.defaultAttemptData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * ExamAttemptStore is a process-wide singleton object used to hand off a freshly-generated
 * attempt (e.g. from practice-exam generation) to ExamAttemptScreen without a network round
 * trip. Its only contract is "put then consume once, and consuming clears it."
 */
class ExamAttemptStoreTest {

    @Before
    fun drainAnyLeftoverStash() {
        ExamAttemptStore.consume()
    }

    @Test
    fun `consume returns null when nothing was stashed`() {
        assertNull(ExamAttemptStore.consume())
    }

    @Test
    fun `consume returns the stashed value exactly once`() {
        val data = defaultAttemptData()
        ExamAttemptStore.put(data)

        assertEquals(data, ExamAttemptStore.consume())
        assertNull(ExamAttemptStore.consume())
    }

    @Test
    fun `putting a new value overwrites a previously unconsumed one`() {
        ExamAttemptStore.put(defaultAttemptData(questions = listOf(com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.fakeQuestion(mcqId = "old"))))
        ExamAttemptStore.put(defaultAttemptData(questions = listOf(com.prostuti.student.data.repository.fake.FakeExamAttemptRepositoryForEngineTest.Companion.fakeQuestion(mcqId = "new"))))

        assertEquals("new", ExamAttemptStore.consume()?.questions?.first()?.mcqId)
    }
}
