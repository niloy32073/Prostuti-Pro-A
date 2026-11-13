package com.prostuti.student.domain.widget

import com.prostuti.student.data.model.response.ApiResponse
import com.prostuti.student.data.model.response.RoutineWithSlots
import com.prostuti.student.data.repository.fake.FakeDataStoreStorageRepository
import com.prostuti.student.data.repository.fake.FakeGroupRepository
import com.prostuti.student.data.repository.fake.FakeGroupRepository.Companion.group
import com.prostuti.student.data.repository.fake.FakeLiveExamRepositoryForTest
import com.prostuti.student.data.repository.fake.FakeQbCurriculumRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.routine
import com.prostuti.student.data.repository.fake.FakeRoutineRepository.Companion.slot
import com.prostuti.student.data.repository.fake.FakeUserRepository
import com.prostuti.student.data.repository.fake.fakeTimedExam
import com.prostuti.student.dataStore.PrefKeys
import com.prostuti.student.domain.FeatureAccess
import com.prostuti.student.domain.FetchLiveExamListUseCase
import com.prostuti.student.domain.GetMeUseCase
import com.prostuti.student.domain.group.FetchMyGroupsUseCase
import com.prostuti.student.domain.routine.DescribeTopicGroupingUseCase
import com.prostuti.student.domain.routine.FetchActiveRoutineUseCase
import com.prostuti.student.domain.routine.FetchSlotsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** "Now" is Tue 10 Nov 2026, 20:00 Dhaka. */
class LoadWidgetDataUseCaseTest {
    private val now = Instant.parse("2026-11-10T14:00:00Z")
    private val storage = FakeDataStoreStorageRepository()
    private val users = FakeUserRepository()
    private val live = FakeLiveExamRepositoryForTest(liveExamListResult = ApiResponse.Success(emptyList()))
    private val routines = FakeRoutineRepository()
    private val groups = FakeGroupRepository()

    private val load = LoadWidgetDataUseCase(
        storage, GetMeUseCase(users), FetchLiveExamListUseCase(live), FetchSlotsUseCase(routines),
        FetchActiveRoutineUseCase(routines), FetchMyGroupsUseCase(groups), DescribeTopicGroupingUseCase(FakeQbCurriculumRepository()),
    )

    private suspend fun signIn(scopes: List<String> = listOf(FeatureAccess.SCOPE_ROUTINE, FeatureAccess.SCOPE_GROUP)) {
        storage.putString(PrefKeys.JWT_TOKEN, "token")
        users.results = listOf(ApiResponse.Success(FakeUserRepository.me().copy(scopes = scopes)))
    }

    @Test fun `signed out asks the server nothing`() = runTest {
        assertEquals(WidgetData.SignedOut, load(now))
        assertTrue(routines.slotsAsked.isEmpty())
        assertTrue(routines.activeAsked.isEmpty())
    }

    @Test fun `signed in merges live and group exams and lists today`() = runTest {
        signIn()
        live.liveExamListResult = ApiResponse.Success(listOf(
            fakeTimedExam(examId = "live", windowStartEpochMillis = Instant.parse("2026-11-11T03:00:00Z").toEpochMilli(), windowEndEpochMillis = Instant.parse("2026-11-11T05:00:00Z").toEpochMilli())
        ))
        routines.slotsResult = { _, _, _ -> ApiResponse.Success(listOf(slot(id = "group-now", groupId = "g1"), slot(id = "mine", groupId = null))) }
        routines.activeResult = ApiResponse.Success(RoutineWithSlots(routine(), slots = emptyList()))
        groups.myGroupsPages = mapOf(1 to ApiResponse.Success(listOf(group(id = "g1"))))

        val data = load(now) as WidgetData.SignedIn

        assertEquals(2, data.upcoming?.size)
        assertEquals(listOf("group-now", "mine"), data.today?.map { it.id })
        assertEquals(true, data.hasPersonalRoutine)
        assertTrue("g1" in data.groupNames)
        assertEquals("two weeks from today", Triple("2026-11-10", "2026-11-23", null), routines.slotsAsked.single())
    }

    @Test fun `without the routine scope no routine call is made`() = runTest {
        signIn(scopes = emptyList())

        val data = load(now) as WidgetData.SignedIn

        assertTrue(routines.slotsAsked.isEmpty())
        assertTrue(routines.activeAsked.isEmpty())
        assertNull(data.today)
        assertEquals("live exams still show", emptyList<UpcomingExam>(), data.upcoming)
    }

    @Test fun `a failed call is unknown, not empty`() = runTest {
        signIn()
        live.liveExamListResult = ApiResponse.Failure("offline", "NETWORK_ERROR")
        routines.slotsResult = { _, _, _ -> ApiResponse.Failure("offline", "NETWORK_ERROR") }
        routines.activeResult = ApiResponse.Failure("offline", "NETWORK_ERROR")

        val data = load(now) as WidgetData.SignedIn

        assertNull(data.upcoming)
        assertNull(data.today)
        assertNull(data.hasPersonalRoutine)
    }
}
